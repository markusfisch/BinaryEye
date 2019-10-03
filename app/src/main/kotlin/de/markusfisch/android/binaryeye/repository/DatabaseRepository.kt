package de.markusfisch.android.binaryeye.repository

import android.content.Context
import android.database.Cursor
import de.markusfisch.android.binaryeye.data.Database

class DatabaseRepository {
	private val db = Database()

	fun open(applicationContext: Context) {
		db.open(applicationContext)
	}

	fun getScan(id: Long): Scan? = getScan(id) { _, cursor ->
		val timeIndex = cursor.getColumnIndex(Database.SCANS_DATETIME)
		val contentIndex = cursor.getColumnIndex(Database.SCANS_CONTENT)
		val rawIndex = cursor.getColumnIndex(Database.SCANS_RAW)
		val formatIndex = cursor.getColumnIndex(Database.SCANS_FORMAT)
		Scan(
			id = id,
			timestamp = cursor.getString(timeIndex),
			content = cursor.getString(contentIndex),
			raw = cursor.getBlob(rawIndex),
			format = cursor.getString(formatIndex)
		)
	}

	fun <T> getScan(id: Long, map: (id: Long, Cursor) -> T): T? = db.getScan(id)?.use { map(id, it.apply { moveToFirst() }) }

	fun getScans(): List<SimpleScan> = getScans { cursor ->
		val idIndex = cursor.getColumnIndex(Database.SCANS_ID)
		val timeIndex = cursor.getColumnIndex(Database.SCANS_DATETIME)
		val contentIndex = cursor.getColumnIndex(Database.SCANS_CONTENT)
		val formatIndex = cursor.getColumnIndex(Database.SCANS_FORMAT)
		SimpleScan(
			id = cursor.getLong(idIndex),
			timestamp = cursor.getString(timeIndex),
			content = cursor.getString(contentIndex),
			format = cursor.getString(formatIndex)
		)
	}

	fun <T> getScans(map: (Cursor) -> T): List<T> = db.getScans()?.use { it.asIterable.map(map) } ?: emptyList()

	fun getScansCursor(): Cursor? = db.getScans()

	fun hasBinaryData(): Boolean = db.hasBinaryData()?.use { it.count > 0 } ?: false

	fun insertScan(
		timestamp: Long,
		content: String,
		raw: ByteArray?,
		format: String
	): Long = db.insertScan(timestamp, content, raw, format)

	fun removeScan(id: Long) = db.removeScan(id)

	fun removeScans() = db.removeScans()

	data class SimpleScan(val id: Long, val timestamp: String, val content: String, val format: String)

	data class Scan(val id: Long, val timestamp: String, val content: String, val raw: ByteArray?, val format: String) {
		private fun Any.escaped(delimiter: String): String {
			val any = this.toString()
			return when {
				any.run { contains('"') || contains('\n') || contains(delimiter) } ->
					"\"${any.replace("\"", "\"\"").replace(delimiter, "\"$delimiter")}\""
				else -> any
			}
		}

		fun toCSV(delimiter: String, allowBinary: Boolean): ByteArray {
			val csvStart = "${timestamp.escaped(delimiter)}$delimiter${content.escaped(delimiter)}$delimiter${format.escaped(delimiter)}".toByteArray()
			val csvRaw = if (allowBinary) delimiter.toByteArray() + (raw ?: ByteArray(0)) else ByteArray(0)
			return csvStart + csvRaw
		}

		// Needed to be overwritten manually, as ByteArray is an array and this isn't handled well by Kotlin
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Scan

			if (id != other.id) return false
			if (timestamp != other.timestamp) return false
			if (content != other.content) return false
			if (raw != null) {
				if (other.raw == null) return false
				if (!raw.contentEquals(other.raw)) return false
			} else if (other.raw != null) return false
			if (format != other.format) return false

			return true
		}

		// Needed to be overwritten manually, as ByteArray is an array and this isn't handled well by Kotlin
		override fun hashCode(): Int {
			var result = id.hashCode()
			result = 31 * result + timestamp.hashCode()
			result = 31 * result + content.hashCode()
			result = 31 * result + (raw?.contentHashCode() ?: 0)
			result = 31 * result + format.hashCode()
			return result
		}
	}
}

private val Cursor.asIterable: Iterable<Cursor>
	get() = object : Iterable<Cursor> {
		override fun iterator(): Iterator<Cursor> = object : Iterator<Cursor> {
			override fun hasNext(): Boolean = position + 1 < count
			override fun next(): Cursor = if (moveToNext()) this@asIterable else throw IllegalStateException("You can't access more items, then there are")
		}
	}
