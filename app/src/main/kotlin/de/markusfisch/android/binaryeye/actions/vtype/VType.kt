package de.markusfisch.android.binaryeye.actions.vtype

object VTypeParser {
	private val vTypeRegex = """^BEGIN:(VCARD|VEVENT)(?:\R.+?:[\s\S]+?)+?\REND:\1\R?$""".toRegex(RegexOption.IGNORE_CASE)
	private val propertyRegex = """^(.+?):([\s\S]*?)$""".toRegex(RegexOption.MULTILINE)

	fun parseVType(data: String): String? = vTypeRegex.matchEntire(data)?.groupValues?.get(1)?.toUpperCase()

	fun parseMap(data: String): Map<String, List<VTypeProperty>> = propertyRegex.findAll(data).map {
		it.groupValues[1].split(';').let { typeAndInfo ->
			typeAndInfo[0] to VTypeProperty(typeAndInfo.drop(1), it.groupValues[2])
		}
	}.groupBy({ it.first.toUpperCase() }) { it.second }
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
		private val typeRegex = """^(?:TYPE=)(.+?)[:;]?.*$""".toRegex(RegexOption.IGNORE_CASE)
	}
}
