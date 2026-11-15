package de.markusfisch.android.binaryeye.content

import java.util.GregorianCalendar
import java.util.Locale

data class Gs1Medication(
	val gtin: String,
	val expiry: String,
	val batch: String,
	val serial: String,
	val nationalIdentifiers: Map<String, String> = emptyMap()
)

/** Parses plain or human-readable GS1 data in medicine Data Matrix codes. */
object Gs1MedicationParser {
	private val element = Regex("\\(([0-9]{2,4})\\)")
	// GS1 AI encodable character set 82, with at most 20 characters.
	private val variableValue = Regex("[!\"%-?A-Z_a-z]{1,20}")
	private const val GROUP_SEPARATOR = '\u001d'

	fun parse(text: String, format: String): Gs1Medication? = parse(
		text,
		format,
		GregorianCalendar().get(GregorianCalendar.YEAR)
	)

	internal fun parse(
		text: String,
		format: String,
		currentYear: Int
	): Gs1Medication? {
		if (format != "DataMatrix") {
			return null
		}

		val content = text.removePrefix("]d2")
		val elements = if (content.startsWith('(')) {
			parseHumanReadableElements(content) ?: return null
		} else {
			parsePlainElements(content) ?: return null
		}
		return medicationFrom(elements, currentYear)
	}

	private fun parseHumanReadableElements(content: String): Map<String, String>? {
		val matches = element.findAll(content).toList()
		if (matches.isEmpty() || matches.first().range.first != 0) {
			return null
		}
		val elements = mutableMapOf<String, String>()
		for ((index, match) in matches.withIndex()) {
			val ai = match.groupValues[1]
			if (ai in elements) {
				return null
			}
			val end = matches.getOrNull(index + 1)?.range?.first ?: content.length
			val value = content.substring(match.range.last + 1, end)
				.trimEnd(GROUP_SEPARATOR)
			if (GROUP_SEPARATOR in value) {
				return null
			}
			elements[ai] = value
		}
		return elements
	}

	private fun parsePlainElements(content: String): Map<String, String>? {
		val elements = mutableMapOf<String, String>()
		var offset = 0
		while (offset < content.length) {
			if (content[offset] == GROUP_SEPARATOR) {
				offset++
				continue
			}
			if (offset + 2 > content.length) {
				return null
			}
			val aiLength = if (content.startsWith("71", offset) &&
				content.getOrNull(offset + 2)?.let { it in '0'..'5' } == true
			) {
				3
			} else {
				2
			}
			if (offset + aiLength > content.length) {
				return null
			}
			val ai = content.substring(offset, offset + aiLength)
			if (!ai.isAsciiDigits() || ai in elements) {
				return null
			}
			offset += aiLength

			// GS1 General Specifications, table 7-6: predefined lengths
			// include the AI and depend only on its first two digits.
			// All other element strings end at a separator or end of input.
			val length = predefinedLength(ai.take(2))?.minus(aiLength)
			val end = if (length != null) {
				if (offset + length > content.length) {
					return null
				}
				offset + length
			} else {
				val separator = content.indexOf(GROUP_SEPARATOR, offset)
				if (separator < 0) content.length else separator
			}
			val value = content.substring(offset, end)
			if (value.isEmpty() || GROUP_SEPARATOR in value) {
				return null
			}
			if (ai == "01" || ai == "17" || ai == "10" || ai == "21" ||
				ai in nationalReimbursementAis
			) {
				elements[ai] = value
			}
			offset = end
		}
		return elements
	}

	private fun medicationFrom(
		elements: Map<String, String>,
		currentYear: Int
	): Gs1Medication? {
		if (elements.any { (ai, value) ->
			(ai == "10" || ai == "21" || ai in nationalReimbursementAis) &&
				!variableValue.matches(value)
		}) {
			return null
		}
		val gtin = elements["01"]?.takeIf(::isValidGtin)
			?: return null

		val expiry = elements["17"]?.let { formatExpiry(it, currentYear) } ?: return null
		val batch = elements["10"] ?: return null
		val serial = elements["21"] ?: return null
		return Gs1Medication(
			gtin,
			expiry,
			batch,
			serial,
			elements.filterKeys { it in nationalReimbursementAis }
		)
	}

	private val nationalReimbursementAis = setOf(
		"710", "711", "712", "713", "714", "715"
	)

	private fun predefinedLength(prefix: String): Int? = when (prefix) {
		"00" -> 20
		"01", "02", "03" -> 16
		"04" -> 18
		in "11".."19" -> 8
		"20" -> 4
		in "31".."36" -> 10
		"41" -> 16
		else -> null
	}

	private fun isValidGtin(value: String): Boolean {
		if (value.length != 14 || !value.isAsciiDigits()) {
			return false
		}
		val sum = value.dropLast(1).reversed().mapIndexed { index, digit ->
			(digit - '0') * if (index % 2 == 0) 3 else 1
		}.sum()
		return (10 - sum % 10) % 10 == value.last() - '0'
	}

	private fun formatExpiry(value: String, currentYear: Int): String? {
		if (value.length != 6 || !value.isAsciiDigits()) {
			return null
		}
		val shortYear = value.substring(0, 2).toInt()
		var year = currentYear / 100 * 100 + shortYear
		if (year < currentYear - 49) {
			year += 100
		} else if (year > currentYear + 50) {
			year -= 100
		}
		val month = value.substring(2, 4).toInt()
		val day = value.substring(4, 6).toInt()
		if (month !in 1..12 || day !in 0..31) {
			return null
		}
		if (day != 0 && day > daysInMonth(year, month)) {
			return null
		}
		val actualDay = if (day == 0) daysInMonth(year, month) else day
		return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, actualDay)
	}

	private fun daysInMonth(year: Int, month: Int): Int = when (month) {
		2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
		4, 6, 9, 11 -> 30
		else -> 31
	}

	private fun String.isAsciiDigits() = all { it in '0'..'9' }
}
