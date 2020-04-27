package de.markusfisch.android.binaryeye.data

import android.content.Context
import android.database.Cursor
import de.markusfisch.android.binaryeye.app.writeExternalFile

fun exportCsv(
	context: Context,
	name: String,
	cursor: Cursor,
	delimiter: String
) {
	if (!cursor.moveToFirst()) {
		return
	}
	val columns = arrayOf(
		Database.SCANS_DATETIME,
		Database.SCANS_FORMAT,
		Database.SCANS_CONTENT,
		Database.SCANS_ERROR_CORRECTION_LEVEL,
		Database.SCANS_ISSUE_NUMBER,
		Database.SCANS_ORIENTATION,
		Database.SCANS_OTHER_META_DATA,
		Database.SCANS_PDF417_EXTRA_METADATA,
		Database.SCANS_POSSIBLE_COUNTRY,
		Database.SCANS_SUGGESTED_PRICE,
		Database.SCANS_UPC_EAN_EXTENSION
	)
	val indices = columns.map {
		cursor.getColumnIndex(it)
	}
	val contentIndex = cursor.getColumnIndex(Database.SCANS_CONTENT)
	val rawIndex = cursor.getColumnIndex(Database.SCANS_RAW)
	writeExternalFile(context, name, "text/csv") { outputStream ->
		outputStream.write(
			columns.joinToString(
				delimiter,
				postfix = "\n"
			).toByteArray()
		)
		do {
			var deviation: Pair<Int, String>? = null
			if (cursor.getString(contentIndex)?.isEmpty() == true) {
				deviation = Pair(
					contentIndex,
					cursor.getBlob(rawIndex).toHexString()
				)
			}
			outputStream.write(
				cursor.toCsvRecord(indices, delimiter, deviation)
			)
		} while (cursor.moveToNext())
	}
}

private fun ByteArray.toHexString(): String {
	val hex = StringBuilder()
	for (i in this.indices) {
		hex.append(String.format("%02X", this[i]))
	}
	return hex.toString()
}

private fun Cursor.toCsvRecord(
	indices: List<Int>,
	delimiter: String,
	deviation: Pair<Int, String>?
): ByteArray {
	val sb = StringBuilder()
	indices.forEach {
		val value = if (deviation?.first == it) {
			deviation.second
		} else {
			this.getString(it)
		}
		sb.append(value?.quoteAndEscape() ?: "")
		sb.append(delimiter)
	}
	sb.append("\n")
	return sb.toString().toByteArray()
}

private fun String.quoteAndEscape() = "\"${this
	.replace("\n", " ")
	.replace("\"", "\"\"")}\""
