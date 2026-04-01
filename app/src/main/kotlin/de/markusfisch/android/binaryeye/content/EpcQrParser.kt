package de.markusfisch.android.binaryeye.content

enum class EpcQrElement {
	SERVICE_TAG, VERSION, CHARSET, IDENTIFICATION,
	BIC, NAME, IBAN, AMOUNT, PURPOSE, REFERENCE, TEXT, INFORMATION
}

data class EpcQrInfo(
	val elements: Map<EpcQrElement, String>
)

object EpcQrParser {
	private val BIC_REGEX = Regex("[A-Z0-9]{8}([A-Z0-9]{3})?")
	private val IBAN_REGEX = Regex("[A-Z]{2}[0-9A-Z]{13,32}")
	private val AMOUNT_REGEX = Regex("EUR\\d{1,10}(\\.\\d{2})?")
	private val PURPOSE_REGEX = Regex("[A-Z]{4}")

	private val charsetMap = mapOf(
		"1" to "UTF-8",
		"2" to "ISO 8859-1",
		"3" to "ISO 8859-2",
		"4" to "ISO 8859-4",
		"5" to "ISO 8859-5",
		"6" to "ISO 8859-7",
		"7" to "ISO 8859-10",
		"8" to "ISO 8859-15"
	)

	private fun splitLines(s: String): List<String>? {
		val normalized = s
			.replace("\r\n", "\n")
			.replace('\r', '\n')
			.removeSuffix("\n")
		if (normalized.isEmpty()) {
			return null
		}
		return normalized.split('\n')
	}

	private fun isValidVersion(value: String): Boolean {
		return value == "001" || value == "002"
	}

	private fun isValidCharset(value: String): Boolean {
		return charsetMap.containsKey(value)
	}

	private fun isValidIdentification(value: String): Boolean {
		return value == "SCT"
	}

	private fun isValidBic(value: String): Boolean {
		return value.matches(BIC_REGEX)
	}

	private fun isValidIban(value: String): Boolean {
		return value.matches(IBAN_REGEX)
	}

	private fun isValidAmount(value: String): Boolean {
		return value.matches(AMOUNT_REGEX)
	}

	fun parse(s: String?): EpcQrInfo? {
		if (s.isNullOrEmpty()) {
			return null
		}

		val lines = splitLines(s) ?: return null
		if (lines.size !in 7..12) {
			return null
		}
		if (lines[0] != "BCD") {
			return null
		}
		if (!isValidVersion(lines[1])) {
			return null
		}
		if (!isValidCharset(lines[2])) {
			return null
		}
		if (!isValidIdentification(lines[3])) {
			return null
		}

		val bic = lines.getOrElse(4) { "" }
		if ((lines[1] == "001" || bic.isNotEmpty()) && !isValidBic(bic)) {
			return null
		}

		val recipient = lines.getOrElse(5) { "" }
		if (recipient.isBlank() || recipient.length > 70) {
			return null
		}

		val iban = lines.getOrElse(6) { "" }
		if (!isValidIban(iban)) {
			return null
		}

		val amount = lines.getOrElse(7) { "" }
		if (amount.isNotEmpty() && !isValidAmount(amount)) {
			return null
		}

		val purpose = lines.getOrElse(8) { "" }
		if (purpose.isNotEmpty() && !purpose.matches(PURPOSE_REGEX)) {
			return null
		}

		val remittanceReference = lines.getOrElse(9) { "" }
		if (remittanceReference.length > 25) {
			return null
		}

		val remittanceText = lines.getOrElse(10) { "" }
		if (remittanceText.length > 140) {
			return null
		}

		if (remittanceReference.isNotEmpty() && remittanceText.isNotEmpty()) {
			return null
		}

		val information = lines.getOrElse(11) { "" }
		if (information.length > 70) {
			return null
		}

		return EpcQrInfo(
			linkedMapOf(
				EpcQrElement.SERVICE_TAG to lines[0],
				EpcQrElement.VERSION to lines[1],
				EpcQrElement.CHARSET to (charsetMap[lines[2]] ?: lines[2]),
				EpcQrElement.IDENTIFICATION to lines[3],
				EpcQrElement.BIC to bic,
				EpcQrElement.NAME to recipient,
				EpcQrElement.IBAN to iban,
				EpcQrElement.AMOUNT to amount.removePrefix("EUR"),
				EpcQrElement.PURPOSE to purpose,
				EpcQrElement.REFERENCE to remittanceReference,
				EpcQrElement.TEXT to remittanceText,
				EpcQrElement.INFORMATION to information
			)
		)
	}
}
