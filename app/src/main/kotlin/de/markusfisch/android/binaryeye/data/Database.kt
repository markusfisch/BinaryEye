package de.markusfisch.android.binaryeye.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.format.DateFormat

class Database {
	private lateinit var db: SQLiteDatabase

	fun open(context: Context) {
		db = OpenHelper(context).writableDatabase
	}

	fun getScans(): Cursor? = db.rawQuery(
		"""SELECT
			$SCANS_ID,
			$SCANS_DATETIME,
			$SCANS_CONTENT,
			$SCANS_FORMAT
			FROM $SCANS
			ORDER BY $SCANS_DATETIME DESC
		""", null
	)

	fun getScan(id: Long): Cursor? = db.rawQuery(
		"""SELECT
			$SCANS_DATETIME,
			$SCANS_CONTENT,
			$SCANS_FORMAT
			FROM $SCANS
			WHERE $SCANS_ID = ?
		""", arrayOf("$id")
	)

	fun insertScan(timestamp: Long, name: String, code: String): Long {
		val cv = ContentValues()
		cv.put(
			SCANS_DATETIME, DateFormat.format(
				"yyyy-MM-dd HH:mm:ss",
				timestamp
			).toString()
		)
		cv.put(SCANS_CONTENT, name)
		cv.put(SCANS_FORMAT, code)
		return db.insert(SCANS, null, cv)
	}

	fun removeScan(id: Long) {
		db.delete(SCANS, "$SCANS_ID = ?", arrayOf("$id"))
	}

	fun removeScans() {
		db.delete(SCANS, null, null)
	}

	private class OpenHelper(context: Context) :
		SQLiteOpenHelper(context, "history.db", null, 1) {
		override fun onCreate(db: SQLiteDatabase) {
			createScans(db)
		}

		override fun onUpgrade(
			db: SQLiteDatabase,
			oldVersion: Int,
			newVersion: Int
		) {
		}
	}

	companion object {
		const val SCANS = "scans"
		const val SCANS_ID = "_id"
		const val SCANS_DATETIME = "_datetime"
		const val SCANS_CONTENT = "content"
		const val SCANS_FORMAT = "format"

		private fun createScans(db: SQLiteDatabase) {
			db.execSQL("DROP TABLE IF EXISTS $SCANS")
			db.execSQL(
				"""CREATE TABLE $SCANS (
					$SCANS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
					$SCANS_DATETIME DATETIME NOT NULL,
					$SCANS_CONTENT TEXT NOT NULL,
					$SCANS_FORMAT TEXT NOT NULL
				)"""
			)
		}
	}
}
