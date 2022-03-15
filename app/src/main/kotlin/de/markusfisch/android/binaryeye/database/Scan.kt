package de.markusfisch.android.binaryeye.database

import android.os.Parcel
import android.os.Parcelable
import android.text.format.DateFormat
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import de.markusfisch.android.binaryeye.app.hasNonPrintableCharacters

data class Scan(
	val content: String,
	val raw: ByteArray?,
	val format: String,
	val errorCorrectionLevel: String?,
	val issueNumber: String?,
	val orientation: String?,
	val otherMetaData: String?,
	val pdf417ExtraMetaData: String?,
	val possibleCountry: String?,
	val suggestedPrice: String?,
	val upcEanExtension: String?,
	val dateTime: String = getDateTime(),
	var id: Long = 0L
) : Parcelable {
	// Needs to be overwritten manually, as ByteArray is an array and
	// this isn't handled well by Kotlin
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
				content == other.content &&
				((raw == null && other.raw == null) ||
						(raw != null && other.raw != null && raw.contentEquals(other.raw))) &&
				format == other.format &&
				errorCorrectionLevel == other.errorCorrectionLevel &&
				issueNumber == other.issueNumber &&
				orientation == other.orientation &&
				otherMetaData == other.otherMetaData &&
				pdf417ExtraMetaData == other.pdf417ExtraMetaData &&
				possibleCountry == other.possibleCountry &&
				suggestedPrice == other.suggestedPrice &&
				upcEanExtension == other.upcEanExtension
	}

	// Needs to be overwritten manually, as ByteArray is an array and
	// this isn't handled well by Kotlin
	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + dateTime.hashCode()
		result = 31 * result + content.hashCode()
		result = 31 * result + (raw?.contentHashCode() ?: 0)
		result = 31 * result + format.hashCode()
		result = 31 * result + (errorCorrectionLevel?.hashCode() ?: 0)
		result = 31 * result + (issueNumber?.hashCode() ?: 0)
		result = 31 * result + (orientation?.hashCode() ?: 0)
		result = 31 * result + (otherMetaData?.hashCode() ?: 0)
		result = 31 * result + (pdf417ExtraMetaData?.hashCode() ?: 0)
		result = 31 * result + (possibleCountry?.hashCode() ?: 0)
		result = 31 * result + (suggestedPrice?.hashCode() ?: 0)
		result = 31 * result + (upcEanExtension?.hashCode() ?: 0)
		return result
	}

	private constructor(parcel: Parcel) : this(
		content = parcel.readString() ?: "",
		raw = parcel.readSizedByteArray(),
		format = parcel.readString() ?: "",
		errorCorrectionLevel = parcel.readString(),
		issueNumber = parcel.readString(),
		orientation = parcel.readString(),
		otherMetaData = parcel.readString(),
		pdf417ExtraMetaData = parcel.readString(),
		possibleCountry = parcel.readString(),
		suggestedPrice = parcel.readString(),
		upcEanExtension = parcel.readString(),
		dateTime = parcel.readString() ?: "",
		id = parcel.readLong()
	)

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.apply {
			writeString(content)
			writeSizedByteArray(raw)
			writeString(format)
			writeString(errorCorrectionLevel)
			writeString(issueNumber)
			writeString(orientation)
			writeString(otherMetaData)
			writeString(pdf417ExtraMetaData)
			writeString(possibleCountry)
			writeString(suggestedPrice)
			writeString(upcEanExtension)
			writeString(dateTime)
			writeLong(id)
		}
	}

	override fun describeContents() = 0

	companion object {
		@JvmField
		val CREATOR = object : Parcelable.Creator<Scan> {
			override fun createFromParcel(parcel: Parcel) = Scan(parcel)
			override fun newArray(size: Int) = arrayOfNulls<Scan>(size)
		}
	}
}

private fun getDateTime(time: Long = System.currentTimeMillis()) = DateFormat.format(
	"yyyy-MM-dd HH:mm:ss",
	time
).toString()

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

fun Result.toScan(): Scan {
	val content: String
	val raw: ByteArray?
	if (text.hasNonPrintableCharacters()) {
		content = ""
		raw = getRawData() ?: text.toByteArray()
	} else {
		content = text
		raw = null
	}
	return Scan(
		content,
		raw,
		barcodeFormat.toString(),
		getMetaString(ResultMetadataType.ERROR_CORRECTION_LEVEL),
		getMetaString(ResultMetadataType.ISSUE_NUMBER),
		getMetaString(ResultMetadataType.ORIENTATION),
		getMetaString(ResultMetadataType.OTHER),
		getMetaString(ResultMetadataType.PDF417_EXTRA_METADATA),
		getMetaString(ResultMetadataType.POSSIBLE_COUNTRY),
		getMetaString(ResultMetadataType.SUGGESTED_PRICE),
		getMetaString(ResultMetadataType.UPC_EAN_EXTENSION)
	)
}

private fun Result.getRawData(): ByteArray? {
	val metadata = resultMetadata ?: return null
	val segments = metadata[ResultMetadataType.BYTE_SEGMENTS] ?: return null
	var bytes = ByteArray(0)
	@Suppress("UNCHECKED_CAST")
	for (seg in segments as Iterable<ByteArray>) {
		bytes += seg
	}
	// If the byte segments are shorter than the converted string, the
	// content of the QR Code has been encoded with different encoding
	// modes (e.g. some parts in alphanumeric, some in byte encoding).
	// This is because Zxing only records byte segments for byte encoded
	// parts. Please note the byte segments can actually be longer than
	// the string because Zxing cuts off prefixes like "WIFI:".
	return if (bytes.size >= text.length) bytes else null
}

private fun Result.getMetaString(
	key: ResultMetadataType
): String? = this.resultMetadata?.let {
	it[key]?.toString()
}
