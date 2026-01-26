package de.markusfisch.android.binaryeye.database

import android.content.Context
import android.database.Cursor
import de.markusfisch.android.binaryeye.io.writeExternalFile
import java.io.ByteArrayOutputStream
import java.io.OutputStream

fun Context.exportCsv(
	name: String,
	cursor: Cursor,
	delimiter: String
) = writeExternalFile(name, "text/csv") { outputStream ->
	exportCsv(outputStream, cursor, delimiter)
}

fun Cursor.exportCsv(delimiter: String): String {
	val outputStream = ByteArrayOutputStream()
	exportCsv(outputStream, this, delimiter)
	return outputStream.toString()
}

private fun exportCsv(
	outputStream: OutputStream,
	cursor: Cursor,
	delimiter: String
) {
	if (!cursor.moveToFirst()) {
		return
	}
	val columns = arrayOf(
		Database.SCANS_DATETIME,
		Database.SCANS_FORMAT,
		Database.SCANS_NAME,
		Database.SCANS_TEXT,
		Database.SCANS_ERROR_CORRECTION_LEVEL,
		Database.SCANS_VERSION,
		Database.SCANS_SEQUENCE_SIZE,
		Database.SCANS_SEQUENCE_INDEX,
		Database.SCANS_SEQUENCE_ID,
		Database.SCANS_GTIN_COUNTRY,
		Database.SCANS_GTIN_ADD_ON,
		Database.SCANS_GTIN_PRICE,
		Database.SCANS_GTIN_ISSUE_NUMBER
	)
	val indices = columns.map {
		cursor.getColumnIndex(it)
	}
	val contentIndex = cursor.getColumnIndex(Database.SCANS_TEXT)
	val rawIndex = cursor.getColumnIndex(Database.SCANS_RAW)
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

private fun String.quoteAndEscape() = "\"${
	this
		.replace("\n", " ")
		.replace("\"", "\"\"")
}\""
