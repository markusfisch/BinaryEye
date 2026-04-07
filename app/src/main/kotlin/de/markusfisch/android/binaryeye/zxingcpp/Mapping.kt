package de.markusfisch.android.binaryeye.zxingcpp

val oldToNewFormatNames = mapOf(
	"NONE" to "None",
	"AZTEC" to "Aztec",
	"CODABAR" to "Codabar",
	"CODE_39" to "Code39",
	"CODE_39_EXT" to "Code39Ext",
	"CODE_32" to "Code32",
	"PZN" to "PZN",
	"CODE_93" to "Code93",
	"CODE_128" to "Code128",
	"DATA_BAR_OMNI" to "DataBarOmni",
	"DATA_BAR_STK" to "DataBarStk",
	"DATA_BAR_LTD" to "DataBarLtd",
	"DATA_BAR_EXPANDED" to "DataBarExp",
	"DATA_BAR_EXP_STK" to "DataBarExpStk",
	"DATA_MATRIX" to "DataMatrix",
	"DX_FILM_EDGE" to "DXFilmEdge",
	"EAN_8" to "EAN8",
	"EAN_13" to "EAN13",
	"ITF" to "ITF",
	"MAXICODE" to "MaxiCode",
	"PDF_417" to "PDF417",
	"QR_CODE" to "QRCode",
	"MICRO_QR_CODE" to "MicroQRCode",
	"RMQR_CODE" to "RMQRCode",
	"UPC_A" to "UPCA",
	"UPC_E" to "UPCE"
)

/**
 * Translate legacy barcode format names from ZXingC++ before 3.x
 * to current BarcodeFormat enum names.
 */
fun String.migrateBarcodeFormatName() = oldToNewFormatNames[this] ?: this
