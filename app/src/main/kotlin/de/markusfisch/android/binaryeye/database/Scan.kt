package de.markusfisch.android.binaryeye.database

import android.os.Bundle
import android.text.format.DateFormat
import de.markusfisch.android.binaryeye.zxingcpp.migrateBarcodeFormatName
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import de.markusfisch.android.zxingcpp.ZxingCpp.BitMatrix
import de.markusfisch.android.zxingcpp.ZxingCpp.ContentType
import de.markusfisch.android.zxingcpp.ZxingCpp.Result
import java.util.Locale

data class Scan(
	val text: String,
	val raw: ByteArray?,
	val format: BarcodeFormat,
	val errorCorrectionLevel: String? = null,
	val version: String? = null,
	val dataMask: Int = -1,
	val symbol: BitMatrix? = null,
	val sequenceSize: Int = -1,
	val sequenceIndex: Int = -1,
	val sequenceId: String = "",
	val country: String? = null,
	val addOn: String? = null,
	val price: String? = null,
	val issueNumber: String? = null,
	val dateTime: String = getDateTime(),
	var id: Long = 0L,
	val label: String? = null,
	val pinned: Boolean = false
) {
	// Needs to be overwritten manually, as ByteArray is an array and
	// this isn't handled well by Kotlin.
	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (javaClass != other?.javaClass) {
			return false
		}

		other as Scan

		return id == other.id &&
				dateTime == other.dateTime &&
				text == other.text &&
				((raw == null && other.raw == null) ||
						(raw != null && other.raw != null &&
								raw.contentEquals(other.raw))) &&
				format == other.format &&
				errorCorrectionLevel == other.errorCorrectionLevel &&
				version == other.version &&
				dataMask == other.dataMask &&
				sequenceSize == other.sequenceSize &&
				sequenceIndex == other.sequenceIndex &&
				sequenceId == other.sequenceId &&
				country == other.country &&
				addOn == other.addOn &&
				price == other.price &&
				issueNumber == other.issueNumber &&
				label == other.label &&
				pinned == other.pinned
	}

	// Needs to be overwritten manually, as ByteArray is an array and
	// this isn't handled well by Kotlin.
	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + dateTime.hashCode()
		result = 31 * result + text.hashCode()
		result = 31 * result + (raw?.contentHashCode() ?: 0)
		result = 31 * result + format.hashCode()
		result = 31 * result + (errorCorrectionLevel?.hashCode() ?: 0)
		result = 31 * result + version.hashCode()
		result = 31 * result + dataMask
		result = 31 * result + sequenceSize
		result = 31 * result + sequenceIndex
		result = 31 * result + sequenceId.hashCode()
		result = 31 * result + (country?.hashCode() ?: 0)
		result = 31 * result + (addOn?.hashCode() ?: 0)
		result = 31 * result + (price?.hashCode() ?: 0)
		result = 31 * result + (issueNumber?.hashCode() ?: 0)
		result = 31 * result + (label?.hashCode() ?: 0)
		result = 31 * result + pinned.hashCode()
		return result
	}

}

fun Result.toScan(): Scan {
	return Scan(
		when (contentType) {
			ContentType.GS1,
			ContentType.ISO15434,
			ContentType.TEXT -> text

			else -> "" // Show as binary.
		},
		rawBytes,
		format,
		ecLevel,
		version,
		dataMask,
		symbol,
		sequenceSize,
		sequenceIndex,
		sequenceId,
		gtin?.country,
		gtin?.addOn,
		gtin?.price,
		gtin?.issueNumber
	)
}

fun Scan.toBundle() = Bundle().apply {
	putString(SCAN_TEXT, text)
	putByteArray(SCAN_RAW, raw)
	putString(SCAN_FORMAT, format.name)
	putString(SCAN_ERROR_CORRECTION_LEVEL, errorCorrectionLevel)
	putString(SCAN_VERSION, version)
	putInt(SCAN_DATA_MASK, dataMask)
	putInt(SCAN_SYMBOL_WIDTH, symbol?.width ?: 0)
	putInt(SCAN_SYMBOL_HEIGHT, symbol?.height ?: 0)
	putByteArray(SCAN_SYMBOL_DATA, symbol?.data)
	putInt(SCAN_SEQUENCE_SIZE, sequenceSize)
	putInt(SCAN_SEQUENCE_INDEX, sequenceIndex)
	putString(SCAN_SEQUENCE_ID, sequenceId)
	putString(SCAN_COUNTRY, country)
	putString(SCAN_ADD_ON, addOn)
	putString(SCAN_PRICE, price)
	putString(SCAN_ISSUE_NUMBER, issueNumber)
	putString(SCAN_DATE_TIME, dateTime)
	putLong(SCAN_ID, id)
	putString(SCAN_LABEL, label)
	putBoolean(SCAN_PINNED, pinned)
}

fun Bundle.toScan(): Scan? {
	val formatName = getString(SCAN_FORMAT)?.migrateBarcodeFormatName()
	if (formatName.isNullOrEmpty()) {
		return null
	}
	return try {
		Scan(
			text = getString(SCAN_TEXT) ?: "",
			raw = getByteArray(SCAN_RAW),
			format = BarcodeFormat.valueOf(formatName),
			errorCorrectionLevel = getString(SCAN_ERROR_CORRECTION_LEVEL),
			version = getString(SCAN_VERSION),
			dataMask = getInt(SCAN_DATA_MASK, -1),
			symbol = getBitMatrix(),
			sequenceSize = getInt(SCAN_SEQUENCE_SIZE, -1),
			sequenceIndex = getInt(SCAN_SEQUENCE_INDEX, -1),
			sequenceId = getString(SCAN_SEQUENCE_ID) ?: "",
			country = getString(SCAN_COUNTRY),
			addOn = getString(SCAN_ADD_ON),
			price = getString(SCAN_PRICE),
			issueNumber = getString(SCAN_ISSUE_NUMBER),
			dateTime = getString(SCAN_DATE_TIME) ?: getDateTime(),
			id = getLong(SCAN_ID, 0L),
			label = getString(SCAN_LABEL),
			pinned = getBoolean(SCAN_PINNED, false)
		)
	} catch (_: IllegalArgumentException) {
		null
	}
}

private fun getDateTime(
	time: Long = System.currentTimeMillis()
) = DateFormat.format(
	"yyyy-MM-dd HH:mm:ss",
	time
).toString() + String.format(
	Locale.getDefault(),
	":%03d",
	time % 1000
)

private fun Bundle.getBitMatrix(): BitMatrix? {
	val width = getInt(SCAN_SYMBOL_WIDTH, 0)
	val height = getInt(SCAN_SYMBOL_HEIGHT, 0)
	val data = getByteArray(SCAN_SYMBOL_DATA)
	if (width < 1 || height < 1 || data == null) {
		return null
	}
	return BitMatrix(width, height, data)
}

private const val SCAN_TEXT = "text"
private const val SCAN_RAW = "raw"
private const val SCAN_FORMAT = "format"
private const val SCAN_ERROR_CORRECTION_LEVEL = "error_correction_level"
private const val SCAN_VERSION = "version"
private const val SCAN_DATA_MASK = "data_mask"
private const val SCAN_SYMBOL_WIDTH = "symbol_width"
private const val SCAN_SYMBOL_HEIGHT = "symbol_height"
private const val SCAN_SYMBOL_DATA = "symbol_data"
private const val SCAN_SEQUENCE_SIZE = "sequence_size"
private const val SCAN_SEQUENCE_INDEX = "sequence_index"
private const val SCAN_SEQUENCE_ID = "sequence_id"
private const val SCAN_COUNTRY = "country"
private const val SCAN_ADD_ON = "add_on"
private const val SCAN_PRICE = "price"
private const val SCAN_ISSUE_NUMBER = "issue_number"
private const val SCAN_DATE_TIME = "date_time"
private const val SCAN_ID = "id"
private const val SCAN_LABEL = "label"
private const val SCAN_PINNED = "pinned"
