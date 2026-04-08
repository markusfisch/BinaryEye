package de.markusfisch.android.binaryeye.content

import android.content.Context
import de.markusfisch.android.binaryeye.R
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.idb.IdbSeal

data class SealField(val name: Any, val value: String, val indent: Int = 0)

object SealParser {
	fun parse(
		context: Context,
		bytes: ByteArray
	): List<SealField>? = try {
		context.buildFields(
			Seal.fromString(bytes.toString(Charsets.ISO_8859_1))
		)
	} catch (_: Exception) {
		null
	}

	private fun Context.buildFields(seal: Seal): List<SealField> {
		val fields = mutableListOf<SealField>()
		fields.add(SealField(R.string.vds_document_type, seal.documentType))
		fields.add(SealField(R.string.vds_issuing_country, seal.issuingCountry))
		seal.signatureInfo?.signingDate?.let {
			fields.add(SealField(R.string.vds_signing_date, it.toString()))
		}
		for (message in seal.messageList) {
			val value = when (val v = message.value) {
				is MessageValue.BytesValue -> "${v.rawBytes.size} bytes"
				else -> v.toString()
			}
			if (value.isNotBlank()) {
				fields.add(SealField(message.name, value))
			}
		}
		if (seal is IdbSeal) {
			fields.addAll(IdbVerifier.verify(this, seal))
		}
		return fields
	}
}
