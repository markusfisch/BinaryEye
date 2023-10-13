package de.markusfisch.android.binaryeye.actions.vtype

import java.util.Locale

object VTypeParser {
	private const val BACKSLASH_R_LEGACY =
		"(?:\u000D\u000A|[\u000A\u000B\u000C\u000D\u0085\u2028\u2029])"
	private val vTypeRegex =
		"""^BEGIN:(V.+)(?:$BACKSLASH_R_LEGACY.+?:[\s\S]+?)+?${BACKSLASH_R_LEGACY}END:\1$BACKSLASH_R_LEGACY?$""".toRegex(
			RegexOption.IGNORE_CASE
		)
	private val propertyRegex = """^(.+?):([\s\S]*?)$""".toRegex(
		RegexOption.MULTILINE
	)

	fun parseVType(data: String): String? = vTypeRegex.matchEntire(
		data
	)?.groupValues?.get(1)?.uppercase(Locale.US)

	fun parseMap(
		data: String
	): Map<String, List<VTypeProperty>> = propertyRegex.findAll(data).map {
		it.groupValues[1].split(';').let { typeAndInfo ->
			typeAndInfo[0] to VTypeProperty(
				typeAndInfo.drop(1),
				it.groupValues[2]
			)
		}
	}.groupBy({ it.first.uppercase(Locale.US) }) { it.second }
}

data class VTypeProperty(
	val info: List<String>,
	val value: String
) {
	val firstTypeOrFirstInfo: String?
		get() = this.info.find { it.startsWith("TYPE=", true) }?.let {
			typeRegex.matchEntire(it)?.groupValues?.get(1)
		} ?: this.info.firstOrNull()

	companion object {
		private val typeRegex = """^TYPE=([A-Z_]+)[:;]?.*$""".toRegex(RegexOption.IGNORE_CASE)
	}
}
