package de.markusfisch.android.binaryeye.preference

import org.json.JSONArray
import org.json.JSONObject

data class IgnoreCode(
	val pattern: String,
	val format: String? = null
) {
	private val compiledRegex: Regex? by lazy {
		try {
			Regex(pattern)
		} catch (_: Exception) {
			null
		}
	}

	fun matches(content: String, format: String): Boolean =
		(this.format == null || this.format == format) &&
				compiledRegex?.containsMatchIn(content) == true

	fun toJson(): JSONObject = JSONObject().apply {
		put(KEY_PATTERN, pattern)
		put(KEY_FORMAT, format ?: "")
	}

	companion object {
		private const val KEY_PATTERN = "pattern"
		private const val KEY_FORMAT = "format"

		fun fromJson(obj: JSONObject): IgnoreCode? {
			val pattern = obj.optString(KEY_PATTERN, "").trim()
			if (pattern.isEmpty()) {
				return null
			}
			val format = obj.optString(KEY_FORMAT, "").trim()
			return IgnoreCode(
				pattern = pattern,
				format = format.ifEmpty { null }
			)
		}

		fun fromJsonArray(json: String): MutableList<IgnoreCode> {
			val items = ArrayList<IgnoreCode>()
			val array = try {
				JSONArray(json)
			} catch (_: Exception) {
				JSONArray()
			}
			for (i in 0 until array.length()) {
				when (val item = array.opt(i)) {
					is String -> {
						val pattern = item.trim()
						if (pattern.isNotEmpty()) {
							items.add(IgnoreCode(pattern))
						}
					}

					is JSONObject -> {
						fromJson(item)?.let { items.add(it) }
					}
				}
			}
			return items
		}

		fun toJsonArray(items: List<IgnoreCode>): String {
			val array = JSONArray()
			items.forEach { item ->
				array.put(item.toJson())
			}
			return array.toString()
		}
	}
}
