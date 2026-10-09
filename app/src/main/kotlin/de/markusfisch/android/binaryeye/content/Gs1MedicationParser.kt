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
	private val element = Regex("\\((\\d{2,4})\\)([^()\\u001d]*)")
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
		if (format != "DataMatrix") return null

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
		if (matches.isEmpty() ||
			content.substring(0, matches.first().range.first).isNotEmpty()
		) {
			return null
		}
		if (matches.zipWithNext().any { (first, second) ->
			content.substring(first.range.last + 1, second.range.first)
				.any { it != GROUP_SEPARATOR }
		}) {
			return null
		}
		if (content.substring(matches.last().range.last + 1).isNotEmpty()) return null

		return matches.associate { match ->
			match.groupValues[1] to match.groupValues[2]
		}
			.takeIf { it.size == matches.size }
	}

	private fun parsePlainElements(content: String): Map<String, String>? {
		val elements = mutableMapOf<String, String>()
		var offset = 0
		while (offset < content.length) {
			if (content[offset] == GROUP_SEPARATOR) {
				offset++
				continue
			}
			if (offset + 2 > content.length) return null
			val aiLength = if (content.startsWith("71", offset) &&
				content.getOrNull(offset + 2)?.let { it in '0'..'5' } == true
			) {
				3
			} else {
				2
			}
			if (offset + aiLength > content.length) return null
			val ai = content.substring(offset, offset + aiLength)
			if (!ai.isAsciiDigits() || ai in elements) return null
			offset += aiLength

			val length = when (ai) {
				"01" -> 14
				"17" -> 6
				"10", "21" -> null
				in nationalReimbursementAis -> null
				else -> return if (hasMedicineIdentifiers(elements)) elements else null
			}
			val end = if (length != null) {
				if (offset + length > content.length) return null
				offset + length
			} else {
				val separator = content.indexOf(GROUP_SEPARATOR, offset)
				if (separator < 0) content.length else separator
			}
			val value = content.substring(offset, end)
			if (value.isEmpty() || (length == null && value.length > 20)) return null
			elements[ai] = value
			offset = end
		}
		return elements
	}

	private fun medicationFrom(
		elements: Map<String, String>,
		currentYear: Int
	): Gs1Medication? {
		val gtin = elements["01"]?.takeIf(::isValidGtin)
			?: return null

		val expiry = elements["17"]?.let { formatExpiry(it, currentYear) } ?: return null
		val batch = elements["10"]?.takeIf { it.isNotEmpty() && it.length <= 20 }
			?: return null
		val serial = elements["21"]?.takeIf { it.isNotEmpty() && it.length <= 20 }
			?: return null
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

	private fun hasMedicineIdentifiers(elements: Map<String, String>) =
		elements.keys.containsAll(setOf("01", "17", "10", "21"))

	private fun isValidGtin(value: String): Boolean {
		if (value.length != 14 || !value.isAsciiDigits()) return false
		val sum = value.dropLast(1).reversed().mapIndexed { index, digit ->
			(digit - '0') * if (index % 2 == 0) 3 else 1
		}.sum()
		return (10 - sum % 10) % 10 == value.last() - '0'
	}

	private fun formatExpiry(value: String, currentYear: Int): String? {
		if (value.length != 6 || !value.isAsciiDigits()) return null
		val shortYear = value.substring(0, 2).toInt()
		var year = currentYear / 100 * 100 + shortYear
		if (year < currentYear - 49) {
			year += 100
		} else if (year > currentYear + 50) {
			year -= 100
		}
		val month = value.substring(2, 4).toInt()
		val day = value.substring(4, 6).toInt()
		if (month !in 1..12 || day !in 0..31) return null
		if (day != 0 && day > daysInMonth(year, month)) return null
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
