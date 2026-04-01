package de.markusfisch.android.binaryeye.content

import android.content.Context
import de.markusfisch.android.binaryeye.R

private val elementToLabel = mapOf(
	EpcQrElement.SERVICE_TAG to R.string.epc_service_tag,
	EpcQrElement.VERSION to R.string.epc_version,
	EpcQrElement.CHARSET to R.string.epc_charset,
	EpcQrElement.IDENTIFICATION to R.string.epc_identification,
	EpcQrElement.BIC to R.string.epc_bic,
	EpcQrElement.NAME to R.string.epc_name,
	EpcQrElement.IBAN to R.string.epc_iban,
	EpcQrElement.AMOUNT to R.string.epc_amount,
	EpcQrElement.PURPOSE to R.string.epc_purpose,
	EpcQrElement.REFERENCE to R.string.epc_reference,
	EpcQrElement.TEXT to R.string.epc_text,
	EpcQrElement.INFORMATION to R.string.epc_information
)

fun Context.epcQrToRes(element: EpcQrElement): String {
	return elementToLabel[element]?.let { getString(it) } ?: element.name
}
