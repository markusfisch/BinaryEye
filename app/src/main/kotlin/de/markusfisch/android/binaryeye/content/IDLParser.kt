package de.markusfisch.android.binaryeye.content

data class IDL(
	var iin: String? = null,
	val elements: MutableMap<String, String> = linkedMapOf()
)

object IDLParser {
	private fun resolveSex(value: String): String = when (value.firstOrNull()) {
		'1' -> "M"
		'2' -> "F"
		'9' -> "X"
		else -> value
	}

	private fun skipWhitespace(s: String, p: Int): Int {
		var i = p
		while (i < s.length && s[i].isWhitespace()) {
			++i;
		}
		return i
	}

	private fun findSubType(s: String, idl: IDL): String {
		val len = s.length
		var p = skipWhitespace(s, 0)

		if (p < len && s[p] == '@') {
			while (p < len) {
				if (!s[p].isDigit()) {
					++p
					continue
				}
				var d = p
				while (d < len && s[d].isDigit()) {
					++d
				}
				if (d - p < 8 || len - d < 3) {
					p = d
					continue
				}

				val code = s.substring(d, d + 2)
				if (code != "DL" && code != "ID") {
					p = d + 2
					continue
				}

				idl.elements["DL"] = code
				d += 2

				while (d < len) {
					while (d < len && s[d] !in "DI") {
						++d
					}
					if (len - d < 2) break

					val sec = s.substring(d, d + 2)
					if (sec == "DL" || sec == "ID") {
						return s.substring(d + 2)
					}
					++d
				}
				break
			}
		}
		return s
	}

	private fun findIIN(s: String): String? {
		val ansi = s.indexOf("ANSI").takeIf { it >= 0 } ?: return null
		var p = ansi + 4
		val len = s.length
		val whitespaceSkipped = skipWhitespace(s, p)
		if (whitespaceSkipped == p) {
			return null
		}
		p = whitespaceSkipped
		val iinStart = p
		var digits = 0
		while (digits < 6 && p < len && s[p].isDigit()) {
			++p
			++digits
		}
		return s.substring(iinStart, p)
	}

	fun parse(s: String?): IDL? {
		if (s.isNullOrEmpty()) {
			return null
		}

		val idl = IDL().apply {
			iin = findIIN(s)
		}
		val data = findSubType(s, idl)
		val len = data.length
		var start = 0

		for (p in 0..len) {
			if (p < len && data[p] > '\u001f') {
				continue
			}

			if (p - start > 3) {
				val vp = start + 3
				val key = data.substring(start, vp)

				if (key.matches(Regex("[DZ][A-Z]{2}"))) {
					var value = data.substring(vp, p)
					if (key == "DBC") {
						value = resolveSex(value)
					}
					idl.elements[key] = value.trim()
				}
			}
			start = p + 1
		}

		return if (idl.elements.isEmpty()) null else idl
	}
}
