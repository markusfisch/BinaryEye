package de.markusfisch.android.binaryeye.zxingcpp

/**
 * Translate legacy barcode format names from ZXingC++ before 3.x
 * to current BarcodeFormat enum names.
 */
fun String.migrateBarcodeFormatName() = when (this) {
	"NONE" -> "None"
	"AZTEC" -> "Aztec"
	"CODABAR" -> "Codabar"
	"CODE_39" -> "Code39"
	"CODE_39_EXT" -> "Code39Ext"
	"CODE_32" -> "Code32"
	"PZN" -> "PZN"
	"CODE_93" -> "Code93"
	"CODE_128" -> "Code128"
	"DATA_BAR" -> "DataBar"
	"DATA_BAR_OMNI" -> "DataBarOmni"
	"DATA_BAR_STK" -> "DataBarStk"
	"DATA_BAR_LTD" -> "DataBarLtd"
	"DATA_BAR_EXPANDED" -> "DataBarExp"
	"DATA_BAR_EXP_STK" -> "DataBarExpStk"
	"DATA_MATRIX" -> "DataMatrix"
	"DX_FILM_EDGE" -> "DXFilmEdge"
	"EAN_8" -> "EAN8"
	"EAN_13" -> "EAN13"
	"ITF" -> "ITF"
	"MAXICODE" -> "MaxiCode"
	"PDF_417" -> "PDF417"
	"QR_CODE" -> "QRCode"
	"MICRO_QR_CODE" -> "MicroQRCode"
	"RMQR_CODE" -> "RMQRCode"
	"UPC_A" -> "UPCA"
	"UPC_E" -> "UPCE"
	else -> this
}
