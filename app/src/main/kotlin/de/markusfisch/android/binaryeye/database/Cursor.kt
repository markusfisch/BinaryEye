package de.markusfisch.android.binaryeye.database

import android.database.Cursor

// Overwrite Kotlin's ".use" function for Cursor because Cursor cannot
// be cast to Closeable below API level 16. This should be removed when
// the minSDK is increased to at least JELLY_BEAN (16).
inline fun <R> Cursor.use(block: (Cursor) -> R): R = try {
	block.invoke(this)
} finally {
	close()
}

fun Cursor.getInt(name: String): Int {
	val idx = getColumnIndex(name)
	return if (idx < 0) 0 else getInt(idx)
}

fun Cursor.getLong(name: String): Long {
	val idx = getColumnIndex(name)
	return if (idx < 0) 0L else getLong(idx)
}

fun Cursor.getString(name: String): String {
	val idx = getColumnIndex(name)
	return if (idx < 0) "" else getString(idx) ?: ""
}

fun Cursor.getBlob(name: String): ByteArray? {
	val idx = getColumnIndex(name)
	return if (idx < 0) null else getBlob(idx)
}
