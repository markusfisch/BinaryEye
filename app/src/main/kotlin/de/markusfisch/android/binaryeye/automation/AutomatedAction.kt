package de.markusfisch.android.binaryeye.automation

import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.binaryeye.net.urlEncode
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class AutomatedAction(
	val pattern: String,
	val type: Type,
	val urlTemplate: String? = null,
	val intentUriTemplate: String? = null
) {
	enum class Type {
		Intent, CustomIntent
	}

	fun matches(content: String): Boolean = try {
		Regex(pattern).matches(content)
	} catch (_: Exception) {
		false
	}

	fun toJson(): JSONObject = JSONObject().apply {
		put(KEY_PATTERN, pattern)
		put(KEY_TYPE, type.name.lowercase(Locale.ROOT))
		put(KEY_URL_TEMPLATE, urlTemplate ?: "")
		put(KEY_INTENT_URI_TEMPLATE, intentUriTemplate ?: "")
	}

	companion object {
		private const val KEY_PATTERN = "pattern"
		private const val KEY_TYPE = "type"
		private const val KEY_URL_TEMPLATE = "url_template"
		private const val KEY_INTENT_URI_TEMPLATE = "intent_uri_template"

		fun fromJson(obj: JSONObject): AutomatedAction? {
			val pattern = obj.optString(KEY_PATTERN, "").trim()
			if (pattern.isEmpty()) {
				return null
			}
			val type = when (obj.optString(KEY_TYPE, "")) {
				"custom_intent" -> Type.CustomIntent
				"intent" -> Type.Intent
				"html" -> return null
				else -> Type.Intent
			}
			val urlTemplate = obj.optString(KEY_URL_TEMPLATE, "").trim()
			val intentUriTemplate = obj.optString(
				KEY_INTENT_URI_TEMPLATE,
				""
			).trim()
			return AutomatedAction(
				pattern = pattern,
				type = type,
				urlTemplate = urlTemplate.ifEmpty { null },
				intentUriTemplate = intentUriTemplate.ifEmpty { null }
			)
		}

		fun fromJsonArray(json: String): MutableList<AutomatedAction> {
			val actions = ArrayList<AutomatedAction>()
			val array = try {
				JSONArray(json)
			} catch (_: Exception) {
				JSONArray()
			}
			for (i in 0 until array.length()) {
				val obj = array.optJSONObject(i) ?: continue
				fromJson(obj)?.let { actions.add(it) }
			}
			return actions
		}

		fun toJsonArray(actions: List<AutomatedAction>): String {
			val array = JSONArray()
			actions.forEach { array.put(it.toJson()) }
			return array.toString()
		}
	}
}

fun String.buildUrl(scan: Scan): String {
	return buildTemplate(scan, encodeResult = true)
}

fun String.buildTemplate(scan: Scan, encodeResult: Boolean): String {
	val text = scan.text
	val raw = scan.raw ?: text.toByteArray()
	val result = if (encodeResult) {
		text.urlEncode()
	} else {
		text
	}
	val format = if (encodeResult) {
		scan.format.name.urlEncode()
	} else {
		scan.format.name
	}
	return this
		.replace("{RESULT}", result)
		.replace("{RESULT_BYTES}", raw.toHexString())
		.replace("{FORMAT}", format)
}
