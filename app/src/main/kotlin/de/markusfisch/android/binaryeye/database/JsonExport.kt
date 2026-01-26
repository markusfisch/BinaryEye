package de.markusfisch.android.binaryeye.database

import android.content.Context
import android.database.Cursor
import de.markusfisch.android.binaryeye.io.writeExternalFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

fun Context.exportJson(
	name: String,
	cursor: Cursor
) = writeExternalFile(name, "application/json") { outputStream ->
	cursor.exportJson()?.let {
		outputStream.write(it.toByteArray())
	}
}

fun Cursor.exportJson(): String? {
	if (!moveToFirst()) {
		return null
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
		Pair(getColumnIndex(it), it)
	}
	val contentIndex = getColumnIndex(Database.SCANS_TEXT)
	val rawIndex = getColumnIndex(Database.SCANS_RAW)
	val root = JSONArray()
	do {
		var deviation: Pair<Int, String>? = null
		if (getString(contentIndex)?.isEmpty() == true) {
			deviation = Pair(
				contentIndex,
				getBlob(rawIndex).toHexString()
			)
		}
		root.put(toJsonObject(indices, deviation))
	} while (moveToNext())
	return root.toString()
}

private fun Cursor.toJsonObject(
	indices: List<Pair<Int, String>>,
	deviation: Pair<Int, String>?
): JSONObject {
	val obj = JSONObject()
	return try {
		indices.forEach {
			val value = if (deviation?.first == it.first) {
				deviation.second
			} else {
				this.getString(it.first)
			}
			obj.put(it.second, value ?: "")
		}
		obj
	} catch (_: JSONException) {
		obj
	}
}
