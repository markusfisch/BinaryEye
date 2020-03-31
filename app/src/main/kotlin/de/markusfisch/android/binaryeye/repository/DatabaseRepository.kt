package de.markusfisch.android.binaryeye.repository

import android.content.Context
import android.database.Cursor
import de.markusfisch.android.binaryeye.data.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class DatabaseRepository {
	private val db = Database()

	fun open(applicationContext: Context) {
		db.open(applicationContext)
	}

	fun getScan(id: Long): Scan? = getScan(id) { _, cursor ->
		Scan(
			cursor.getString(Database.SCANS_CONTENT),
			cursor.getBlob(Database.SCANS_RAW),
			cursor.getString(Database.SCANS_FORMAT),
			cursor.getString(
				Database.SCANS_ERROR_CORRECTION_LEVEL
			),
			cursor.getString(Database.SCANS_ISSUE_NUMBER),
			cursor.getString(Database.SCANS_ORIENTATION),
			cursor.getString(Database.SCANS_OTHER_META_DATA),
			cursor.getString(
				Database.SCANS_PDF417_EXTRA_METADATA
			),
			cursor.getString(
				Database.SCANS_POSSIBLE_COUNTRY
			),
			cursor.getString(Database.SCANS_SUGGESTED_PRICE),
			cursor.getString(
				Database.SCANS_UPC_EAN_EXTENSION
			),
			cursor.getString(Database.SCANS_DATETIME),
			id
		)
	}

	private fun <T> getScan(id: Long, map: (id: Long, Cursor) -> T): T? =
		db.getScan(id)?.use { map(id, it.apply { moveToFirst() }) }

	@ExperimentalCoroutinesApi
	fun getScans(): Flow<SimpleScan> = getScans { cursor ->
		SimpleScan(
			cursor.getLong(Database.SCANS_ID),
			cursor.getString(Database.SCANS_DATETIME),
			cursor.getString(Database.SCANS_CONTENT),
			cursor.getString(Database.SCANS_FORMAT)
		)
	}

	@ExperimentalCoroutinesApi
	private fun <T> getScans(map: suspend (Cursor) -> T): Flow<T> = flow {
		db.getScans()?.use { results ->
			results.asIterable.forEach {
				emit(it)
			}
		}
	}.map(map).flowOn(Dispatchers.IO)

	fun getScansCursor(): Cursor? = db.getScans()

	fun hasBinaryData() = db.hasBinaryData()?.use { it.count > 0 } ?: false

	fun insertScan(scan: Scan): Long = db.insertScan(scan)

	fun removeScan(id: Long) = db.removeScan(id)

	fun removeScans() = db.removeScans()

	data class SimpleScan(
		val id: Long,
		val timestamp: String,
		val content: String,
		val format: String
	)
}

private fun Cursor.getString(name: String) = this.getString(
	this.getColumnIndex(name)
)

private fun Cursor.getBlob(name: String) = this.getBlob(
	this.getColumnIndex(name)
)

private fun Cursor.getLong(name: String) = this.getLong(
	this.getColumnIndex(name)
)

private val Cursor.asIterable: Iterable<Cursor>
	get() = object : Iterable<Cursor> {
		override fun iterator(): Iterator<Cursor> = object : Iterator<Cursor> {
			override fun hasNext(): Boolean = position + 1 < count
			override fun next(): Cursor = if (moveToNext()) {
				this@asIterable
			} else {
				throw IllegalStateException("You can't access more items, then there are")
			}
		}
	}
