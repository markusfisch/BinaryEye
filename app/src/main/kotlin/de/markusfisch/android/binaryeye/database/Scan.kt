package de.markusfisch.android.binaryeye.database

import android.os.Parcel
import android.os.Parcelable
import android.text.format.DateFormat
import de.markusfisch.android.binaryeye.app.hasNonPrintableCharacters
import de.markusfisch.android.zxingcpp.ZxingCpp.Result

data class Scan(
	val content: String,
	val raw: ByteArray?,
	val format: String,
	val errorCorrectionLevel: String? = null,
	val versionNumber: Int = 0,
	val sequenceSize: Int = -1,
	val sequenceIndex: Int = -1,
	val sequenceId: String = "",
	val country: String? = null,
	val addOn: String? = null,
	val price: String? = null,
	val issueNumber: String? = null,
	val dateTime: String = getDateTime(),
	var id: Long = 0L
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
				content == other.content &&
				((raw == null && other.raw == null) ||
						(raw != null && other.raw != null && raw.contentEquals(other.raw))) &&
				format == other.format &&
				errorCorrectionLevel == other.errorCorrectionLevel &&
				versionNumber == other.versionNumber &&
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
		result = 31 * result + content.hashCode()
		result = 31 * result + (raw?.contentHashCode() ?: 0)
		result = 31 * result + format.hashCode()
		result = 31 * result + (errorCorrectionLevel?.hashCode() ?: 0)
		result = 31 * result + versionNumber
		result = 31 * result + sequenceSize
		result = 31 * result + sequenceIndex
		result = 31 * result + sequenceId.hashCode()
		result = 31 * result + (country?.hashCode() ?: 0)
		result = 31 * result + (addOn?.hashCode() ?: 0)
		result = 31 * result + (price?.hashCode() ?: 0)
		result = 31 * result + (issueNumber?.hashCode() ?: 0)
		return result
	}

	private constructor(parcel: Parcel) : this(
		content = parcel.readString() ?: "",
		raw = parcel.readSizedByteArray(),
		format = parcel.readString() ?: "",
		errorCorrectionLevel = parcel.readString(),
		versionNumber = parcel.readInt(),
		sequenceSize = parcel.readInt(),
		sequenceIndex = parcel.readInt(),
		sequenceId = parcel.readString() ?: "",
		country = parcel.readString(),
		addOn = parcel.readString(),
		price = parcel.readString(),
		issueNumber = parcel.readString(),
		dateTime = parcel.readString() ?: "",
		id = parcel.readLong()
	)

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.apply {
			writeString(content)
			writeSizedByteArray(raw)
			writeString(format)
			writeString(errorCorrectionLevel)
			writeInt(versionNumber)
			writeInt(sequenceSize)
			writeInt(sequenceIndex)
			writeString(sequenceId)
			writeString(country)
			writeString(addOn)
			writeString(price)
			writeString(issueNumber)
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
		raw = rawBytes
	} else {
		content = text
		raw = null
	}
	return Scan(
		content,
		raw,
		format,
		ecLevel,
		versionNumber,
		sequenceSize,
		sequenceIndex,
		sequenceId,
		gtin?.country,
		gtin?.addOn,
		gtin?.price,
		gtin?.issueNumber
	)
}
