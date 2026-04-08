package de.markusfisch.android.binaryeye.content

import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.generic.Seal

data class VdsField(val name: String, val value: String, val indent: Int = 0)

object VdsParser {
	fun parse(bytes: ByteArray): List<VdsField>? {
		return try {
			val seal = Seal.fromString(bytes.toString(Charsets.ISO_8859_1))
			buildFields(seal)
		} catch (_: Exception) {
			null
		}
	}

	private fun buildFields(seal: Seal): List<VdsField> {
		val fields = mutableListOf<VdsField>()
		fields.add(VdsField("Document Type", seal.documentType))
		fields.add(VdsField("Issuing Country", seal.issuingCountry))
		seal.signatureInfo?.signingDate?.let {
			fields.add(VdsField("Signing Date", it.toString()))
		}
		for (message in seal.messageList) {
			val value = when (val v = message.value) {
				is MessageValue.BytesValue -> "(${v.rawBytes.size} bytes)"
				else -> v.toString()
			}
			if (value.isNotBlank()) {
				fields.add(VdsField(message.name, value))
			}
		}
		return fields
	}
}
