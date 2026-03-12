package de.markusfisch.android.binaryeye.database

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.format.DateFormat
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import de.markusfisch.android.zxingcpp.ZxingCpp.BitMatrix
import de.markusfisch.android.zxingcpp.ZxingCpp.ContentType
import de.markusfisch.android.zxingcpp.ZxingCpp.Result
import de.markusfisch.android.binaryeye.zxingcpp.migrateBarcodeFormatName
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
	val label: String? = null
) : Parcelable {
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
				issueNumber == other.issueNumber
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
		return result
	}

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.apply {
			writeString(text)
			writeSizedByteArray(raw)
			writeString(format.name)
			writeString(errorCorrectionLevel)
			writeString(version)
			writeInt(dataMask)
			writeBitMatrix(symbol)
			writeInt(sequenceSize)
			writeInt(sequenceIndex)
			writeString(sequenceId)
			writeString(country)
			writeString(addOn)
			writeString(price)
			writeString(issueNumber)
			writeString(dateTime)
			writeLong(id)
			writeString(label)
		}
	}

	override fun describeContents() = 0

	companion object {
		@JvmField
		val CREATOR = object : Parcelable.Creator<Scan> {
			override fun createFromParcel(parcel: Parcel) = readScanFromParcel(
				parcel
			)
			override fun newArray(size: Int) = arrayOfNulls<Scan>(size)
		}
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
			label = getString(SCAN_LABEL)
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

private fun readScanFromParcel(parcel: Parcel): Scan {
	val text = parcel.readString() ?: ""
	val raw = parcel.readSizedByteArray()
	val format = parcel.readBarcodeFormat()
	val errorCorrectionLevel = parcel.readString()
	val version = parcel.readString()
	val dataMask = parcel.readInt()
	val tail = parcel.readScanParcelTail()
	return Scan(
		text = text,
		raw = raw,
		format = format,
		errorCorrectionLevel = errorCorrectionLevel,
		version = version,
		dataMask = dataMask,
		symbol = tail.symbol,
		sequenceSize = tail.sequenceSize,
		sequenceIndex = tail.sequenceIndex,
		sequenceId = tail.sequenceId,
		country = tail.country,
		addOn = tail.addOn,
		price = tail.price,
		issueNumber = tail.issueNumber,
		dateTime = tail.dateTime,
		id = tail.id,
		label = tail.label
	)
}

private data class ScanParcelTail(
	val symbol: BitMatrix?,
	val sequenceSize: Int,
	val sequenceIndex: Int,
	val sequenceId: String,
	val country: String?,
	val addOn: String?,
	val price: String?,
	val issueNumber: String?,
	val dateTime: String,
	val id: Long,
	val label: String?
)

private fun Parcel.readBarcodeFormat(): BarcodeFormat {
	val formatName = (readString() ?: "").migrateBarcodeFormatName()
	return BarcodeFormat.valueOf(formatName)
}

private fun Parcel.readScanParcelTail(): ScanParcelTail {
	val position = dataPosition()
	val tail = readCurrentScanParcelTail()
	if (tail.isPlausible()) {
		return tail
	}
	setDataPosition(position)
	return readLegacyScanParcelTail()
}

private fun Parcel.readCurrentScanParcelTail() = ScanParcelTail(
	symbol = readBitMatrix(),
	sequenceSize = readInt(),
	sequenceIndex = readInt(),
	sequenceId = readString() ?: "",
	country = readString(),
	addOn = readString(),
	price = readString(),
	issueNumber = readString(),
	dateTime = readString() ?: "",
	id = readLong(),
	label = readString()
)

private fun Parcel.readLegacyScanParcelTail() = ScanParcelTail(
	symbol = null,
	sequenceSize = readInt(),
	sequenceIndex = readInt(),
	sequenceId = readString() ?: "",
	country = readString(),
	addOn = readString(),
	price = readString(),
	issueNumber = readString(),
	dateTime = readString() ?: "",
	id = readLong(),
	label = readString()
)

private fun ScanParcelTail.isPlausible(): Boolean {
	if (!dateTime.matches(DATE_TIME_PATTERN)) {
		return false
	}
	if (id < 0L) {
		return false
	}
	if (sequenceSize < -1 || sequenceIndex < -1) {
		return false
	}
	return true
}

private val DATE_TIME_PATTERN = Regex(
	"""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}:\d{3}$"""
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

private fun Parcel.writeSizedByteArray(array: ByteArray?) {
	val size = array?.size ?: 0
	writeInt(size)
	if (size > 0) {
		writeByteArray(array)
	}
}

private fun Parcel.readSizedByteArray(): ByteArray? {
	val size = readInt()
	return if (size > 0) {
		val array = ByteArray(size)
		readByteArray(array)
		array
	} else {
		null
	}
}

private fun Parcel.writeBitMatrix(bm: BitMatrix?) {
	writeInt(bm?.width ?: 0)
	bm?.let {
		writeInt(bm.height)
		writeSizedByteArray(bm.data)
	}
}

private fun Parcel.readBitMatrix(): BitMatrix? {
	val width = readInt()
	if (width < 1) {
		return null
	}
	val height = readInt()
	val data = readSizedByteArray() ?: return null
	return BitMatrix(width, height, data)
}
