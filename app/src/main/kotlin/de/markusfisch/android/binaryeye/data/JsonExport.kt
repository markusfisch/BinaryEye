package de.markusfisch.android.binaryeye.data

import android.content.Context
import android.database.Cursor
import de.markusfisch.android.binaryeye.app.toHexString
import de.markusfisch.android.binaryeye.app.writeExternalFile
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

fun exportJson(context: Context, name: String, cursor: Cursor) {
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
		Pair(cursor.getColumnIndex(it), it)
	}
	val contentIndex = cursor.getColumnIndex(Database.SCANS_CONTENT)
	val rawIndex = cursor.getColumnIndex(Database.SCANS_RAW)
	val root = JSONArray()
	writeExternalFile(context, name, "application/json") { outputStream ->
		do {
			var deviation: Pair<Int, String>? = null
			if (cursor.getString(contentIndex)?.isEmpty() == true) {
				deviation = Pair(
					contentIndex,
					cursor.getBlob(rawIndex).toHexString()
				)
			}
			root.put(cursor.toJsonObject(indices, deviation))
		} while (cursor.moveToNext())
		outputStream.write(root.toString().toByteArray())
	}
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
	} catch (e: JSONException) {
		obj
	}
}
