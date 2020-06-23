package de.markusfisch.android.binaryeye.database

import android.os.Parcel
import android.os.Parcelable
import android.text.format.DateFormat
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType

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
	val timestamp: String = getDateTime(),
	var id: Long = 0L
) : Parcelable {
	constructor(result: Result) : this(
		result.text,
		getRawBytes(result),
		result.barcodeFormat.toString(),
		result.getMetaString(ResultMetadataType.ERROR_CORRECTION_LEVEL),
		result.getMetaString(ResultMetadataType.ISSUE_NUMBER),
		result.getMetaString(ResultMetadataType.ORIENTATION),
		result.getMetaString(ResultMetadataType.OTHER),
		result.getMetaString(ResultMetadataType.PDF417_EXTRA_METADATA),
		result.getMetaString(ResultMetadataType.POSSIBLE_COUNTRY),
		result.getMetaString(ResultMetadataType.SUGGESTED_PRICE),
		result.getMetaString(ResultMetadataType.UPC_EAN_EXTENSION)
	)

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
				timestamp == other.timestamp &&
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
		result = 31 * result + timestamp.hashCode()
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
		raw = readByteArray(parcel),
		format = parcel.readString() ?: "",
		errorCorrectionLevel = parcel.readString(),
		issueNumber = parcel.readString(),
		orientation = parcel.readString(),
		otherMetaData = parcel.readString(),
		pdf417ExtraMetaData = parcel.readString(),
		possibleCountry = parcel.readString(),
		suggestedPrice = parcel.readString(),
		upcEanExtension = parcel.readString(),
		timestamp = parcel.readString() ?: "",
		id = parcel.readLong()
	)

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(content)
		writeByteArray(parcel, raw)
		parcel.writeString(format)
		parcel.writeString(errorCorrectionLevel)
		parcel.writeString(issueNumber)
		parcel.writeString(orientation)
		parcel.writeString(otherMetaData)
		parcel.writeString(pdf417ExtraMetaData)
		parcel.writeString(possibleCountry)
		parcel.writeString(suggestedPrice)
		parcel.writeString(upcEanExtension)
		parcel.writeString(timestamp)
		parcel.writeLong(id)
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

private fun getRawBytes(result: Result): ByteArray? {
	val metadata = result.resultMetadata ?: return null
	val segments = metadata[ResultMetadataType.BYTE_SEGMENTS] ?: return null
	var bytes = ByteArray(0)
	@Suppress("UNCHECKED_CAST")
	for (seg in segments as Iterable<ByteArray>) {
		bytes += seg
	}
	// byte segments can never be shorter than the text.
	// Zxing cuts off content prefixes like "WIFI:"
	return if (bytes.size >= result.text.length) bytes else null
}

private fun Result.getMetaString(
	key: ResultMetadataType
): String? = this.resultMetadata?.let {
	it[key]?.toString()
}

private fun writeByteArray(parcel: Parcel, array: ByteArray?) {
	val size = array?.size ?: 0
	parcel.writeInt(size)
	if (size > 0) {
		parcel.writeByteArray(array)
	}
}

private fun readByteArray(parcel: Parcel): ByteArray? {
	val size = parcel.readInt()
	return if (size > 0) {
		val array = ByteArray(size)
		parcel.readByteArray(array)
		array
	} else {
		null
	}
}
