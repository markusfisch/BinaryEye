package de.markusfisch.android.binaryeye.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.preference.Preferences
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat

class Database {
	private lateinit var db: SQLiteDatabase

	fun open(context: Context) {
		db = OpenHelper(context).writableDatabase
	}

	fun getScans(query: String? = null): Cursor? = db.rawQuery(
		"""SELECT
			$SCANS_ID,
			$SCANS_DATETIME,
			$SCANS_NAME,
			$SCANS_CONTENT,
			$SCANS_FORMAT
			FROM $SCANS
			${getWhereClause(query)}
			ORDER BY $SCANS_DATETIME DESC
		""".trimMargin(), getWhereArguments(query)
	)

	fun getScansDetailed(query: String? = null): Cursor? = db.rawQuery(
		"""SELECT
			$SCANS_ID,
			$SCANS_DATETIME,
			$SCANS_NAME,
			$SCANS_CONTENT,
			$SCANS_RAW,
			$SCANS_FORMAT,
			$SCANS_ERROR_CORRECTION_LEVEL,
			$SCANS_VERSION,
			$SCANS_SEQUENCE_SIZE,
			$SCANS_SEQUENCE_INDEX,
			$SCANS_SEQUENCE_ID,
			$SCANS_GTIN_COUNTRY,
			$SCANS_GTIN_ADD_ON,
			$SCANS_GTIN_PRICE,
			$SCANS_GTIN_ISSUE_NUMBER
			FROM $SCANS
			${getWhereClause(query)}
			ORDER BY $SCANS_DATETIME DESC
		""".trimMargin(), getWhereArguments(query)
	)

	private fun getWhereClause(
		query: String?,
		prefix: String = "WHERE"
	) = if (query?.isNotEmpty() == true) {
		"""$prefix $SCANS_CONTENT LIKE ?
			OR $SCANS_NAME LIKE ?"""
	} else {
		""
	}

	private fun getWhereArguments(
		query: String?
	) = if (query?.isNotEmpty() == true) {
		val instr = "%$query%"
		arrayOf(instr, instr)
	} else {
		null
	}

	fun getScansDetailed(ids: LongArray): Cursor? = db.rawQuery(
		"""SELECT
			$SCANS_ID,
			$SCANS_DATETIME,
			$SCANS_NAME,
			$SCANS_CONTENT,
			$SCANS_RAW,
			$SCANS_FORMAT,
			$SCANS_ERROR_CORRECTION_LEVEL,
			$SCANS_VERSION,
			$SCANS_SEQUENCE_SIZE,
			$SCANS_SEQUENCE_INDEX,
			$SCANS_SEQUENCE_ID,
			$SCANS_GTIN_COUNTRY,
			$SCANS_GTIN_ADD_ON,
			$SCANS_GTIN_PRICE,
			$SCANS_GTIN_ISSUE_NUMBER
			FROM $SCANS
			WHERE $SCANS_ID IN (${ids.joinToString(",")})
			ORDER BY $SCANS_DATETIME DESC
		""".trimMargin(), null
	)

	fun getScan(id: Long): Scan? = db.rawQuery(
		"""SELECT
			$SCANS_ID,
			$SCANS_DATETIME,
			$SCANS_NAME,
			$SCANS_CONTENT,
			$SCANS_RAW,
			$SCANS_FORMAT,
			$SCANS_ERROR_CORRECTION_LEVEL,
			$SCANS_VERSION,
			$SCANS_SEQUENCE_SIZE,
			$SCANS_SEQUENCE_INDEX,
			$SCANS_SEQUENCE_ID,
			$SCANS_GTIN_COUNTRY,
			$SCANS_GTIN_ADD_ON,
			$SCANS_GTIN_PRICE,
			$SCANS_GTIN_ISSUE_NUMBER
			FROM $SCANS
			WHERE $SCANS_ID = ?
		""".trimMargin(), arrayOf("$id")
	)?.use {
		if (it.moveToFirst()) {
			Scan(
				it.getString(SCANS_CONTENT),
				it.getBlob(SCANS_RAW),
				BarcodeFormat.valueOf(it.getString(SCANS_FORMAT)),
				it.getString(SCANS_ERROR_CORRECTION_LEVEL),
				it.getString(SCANS_VERSION),
				it.getInt(SCANS_SEQUENCE_SIZE),
				it.getInt(SCANS_SEQUENCE_INDEX),
				it.getString(SCANS_SEQUENCE_ID),
				it.getString(SCANS_GTIN_COUNTRY),
				it.getString(SCANS_GTIN_ADD_ON),
				it.getString(SCANS_GTIN_PRICE),
				it.getString(SCANS_GTIN_ISSUE_NUMBER),
				it.getString(SCANS_DATETIME),
				it.getLong(SCANS_ID),
				it.getString(SCANS_NAME),
			)
		} else {
			null
		}
	}

	fun insertScan(scan: Scan): Long {
		when (prefs.ignoreDuplicates()) {
			Preferences.Companion.IgnoreDuplicates.Consecutive -> {
				val id = getIdOfLastScan(
					scan.content,
					scan.raw,
					scan.format
				)
				if (id > 0L) {
					return id
				}
			}

			Preferences.Companion.IgnoreDuplicates.Any -> {
				val id = getIdOfScanByContent(scan.content, scan.format)
				if (id > 0L) {
					return id
				}
			}

			else -> Unit
		}
		return db.insert(
			SCANS,
			null,
			ContentValues().apply {
				put(SCANS_DATETIME, scan.dateTime)
				put(SCANS_CONTENT, scan.content)
				if (scan.raw != null) {
					put(SCANS_RAW, scan.raw)
				}
				put(SCANS_FORMAT, scan.format.name)
				scan.errorCorrectionLevel?.let {
					put(SCANS_ERROR_CORRECTION_LEVEL, it)
				}
				put(SCANS_VERSION, scan.version)
				put(SCANS_SEQUENCE_SIZE, scan.sequenceSize)
				put(SCANS_SEQUENCE_INDEX, scan.sequenceIndex)
				put(SCANS_SEQUENCE_ID, scan.sequenceId)
				scan.country?.let { put(SCANS_GTIN_COUNTRY, it) }
				scan.addOn?.let { put(SCANS_GTIN_ADD_ON, it) }
				scan.price?.let { put(SCANS_GTIN_PRICE, it) }
				scan.issueNumber?.let { put(SCANS_GTIN_ISSUE_NUMBER, it) }
			}
		)
	}

	private fun getIdOfLastScan(
		content: String,
		raw: ByteArray?,
		format: BarcodeFormat
	): Long = db.rawQuery(
		"""SELECT
				$SCANS_ID,
				$SCANS_CONTENT,
				$SCANS_RAW,
				$SCANS_FORMAT
				FROM $SCANS
				ORDER BY $SCANS_ID DESC
				LIMIT 1
			""".trimMargin(), null
	)?.use {
		if (it.count > 0 &&
			it.moveToFirst() &&
			it.getString(SCANS_CONTENT) == content &&
			(raw == null || it.getBlob(SCANS_RAW)
				?.contentEquals(raw) == true) &&
			it.getString(SCANS_FORMAT) == format.name
		) {
			it.getLong(SCANS_ID)
		} else {
			0L
		}
	} ?: 0L

	fun getIdOfScanByContent(
		content: String,
		format: BarcodeFormat
	): Long = db.rawQuery(
		"""SELECT
				$SCANS_ID
				FROM $SCANS
				WHERE $SCANS_CONTENT = ?
					AND $SCANS_FORMAT = ?
				LIMIT 1
			""".trimMargin(), arrayOf(content, format.name)
	)?.use {
		if (it.moveToFirst()) {
			it.getLong(SCANS_ID)
		} else {
			0L
		}
	} ?: 0L

	fun removeScan(id: Long) {
		db.delete(SCANS, "$SCANS_ID = ?", arrayOf("$id"))
	}

	fun removeScans(query: String? = null) {
		db.delete(SCANS, getWhereClause(query, ""), getWhereArguments(query))
	}

	fun renameScan(id: Long, name: String) {
		val cv = ContentValues()
		cv.put(SCANS_NAME, name)
		db.update(SCANS, cv, "$SCANS_ID = ?", arrayOf("$id"))
	}

	private class OpenHelper(context: Context) :
		SQLiteOpenHelper(context, FILE_NAME, null, 6) {
		override fun onCreate(db: SQLiteDatabase) {
			db.createScans()
		}

		override fun onUpgrade(
			db: SQLiteDatabase,
			oldVersion: Int,
			newVersion: Int
		) {
			if (oldVersion < 2) {
				db.addRawColumn()
			}
			if (oldVersion < 3) {
				db.addMetaDataColumns()
			}
			if (oldVersion < 4) {
				db.addNameColumn()
			}
			if (oldVersion < 5) {
				db.migrateToZxingCpp()
			}
			if (oldVersion < 6) {
				db.migrateToVersionString()
			}
		}
	}

	companion object {
		const val FILE_NAME = "history.db"
		const val SCANS = "scans"
		const val SCANS_ID = "_id"
		const val SCANS_DATETIME = "_datetime"
		const val SCANS_NAME = "name"
		const val SCANS_CONTENT = "content"
		const val SCANS_RAW = "raw"
		const val SCANS_FORMAT = "format"
		const val SCANS_ERROR_CORRECTION_LEVEL = "error_correction_level"
		const val SCANS_VERSION_NUMBER = "version_number"
		const val SCANS_VERSION = "version"
		const val SCANS_ISSUE_NUMBER = "issue_number"
		const val SCANS_ORIENTATION = "orientation"
		const val SCANS_OTHER_META_DATA = "other_meta_data"
		const val SCANS_PDF417_EXTRA_METADATA = "pdf417_extra_metadata"
		const val SCANS_POSSIBLE_COUNTRY = "possible_country"
		const val SCANS_SUGGESTED_PRICE = "suggested_price"
		const val SCANS_UPC_EAN_EXTENSION = "upc_ean_extension"
		const val SCANS_SEQUENCE_SIZE = "sequence_size"
		const val SCANS_SEQUENCE_INDEX = "sequence_index"
		const val SCANS_SEQUENCE_ID = "sequence_id"
		const val SCANS_GTIN_COUNTRY = "gtin_country"
		const val SCANS_GTIN_ADD_ON = "gtin_add_on"
		const val SCANS_GTIN_PRICE = "gtin_price"
		const val SCANS_GTIN_ISSUE_NUMBER = "gtin_issue_number"

		private fun SQLiteDatabase.createScans() {
			execSQL("DROP TABLE IF EXISTS $SCANS".trimMargin())
			execSQL(
				"""CREATE TABLE $SCANS (
					$SCANS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
					$SCANS_DATETIME DATETIME NOT NULL,
					$SCANS_NAME TEXT,
					$SCANS_CONTENT TEXT NOT NULL,
					$SCANS_RAW BLOB,
					$SCANS_FORMAT TEXT NOT NULL,
					$SCANS_ERROR_CORRECTION_LEVEL TEXT,
					$SCANS_VERSION TEXT,
					$SCANS_SEQUENCE_SIZE INTEGER,
					$SCANS_SEQUENCE_INDEX INTEGER,
					$SCANS_SEQUENCE_ID TEXT,
					$SCANS_GTIN_COUNTRY TEXT,
					$SCANS_GTIN_ADD_ON TEXT,
					$SCANS_GTIN_PRICE TEXT,
					$SCANS_GTIN_ISSUE_NUMBER TEXT
				)""".trimMargin()
			)
		}

		private fun SQLiteDatabase.addRawColumn() {
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_RAW BLOB".trimMargin())
		}

		private fun SQLiteDatabase.addMetaDataColumns() {
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_ERROR_CORRECTION_LEVEL TEXT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_ISSUE_NUMBER INT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_ORIENTATION INT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_OTHER_META_DATA TEXT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_PDF417_EXTRA_METADATA TEXT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_POSSIBLE_COUNTRY TEXT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_SUGGESTED_PRICE TEXT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_UPC_EAN_EXTENSION TEXT".trimMargin())
		}

		private fun SQLiteDatabase.addNameColumn() {
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_NAME TEXT".trimMargin())
		}

		private fun SQLiteDatabase.migrateToZxingCpp() {
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_VERSION_NUMBER INTEGER".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_SEQUENCE_SIZE INTEGER".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_SEQUENCE_INDEX INTEGER".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_SEQUENCE_ID TEXT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_GTIN_COUNTRY TEXT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_GTIN_ADD_ON TEXT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_GTIN_PRICE TEXT".trimMargin())
			execSQL("ALTER TABLE $SCANS ADD COLUMN $SCANS_GTIN_ISSUE_NUMBER TEXT".trimMargin())
			execSQL("UPDATE $SCANS SET $SCANS_SEQUENCE_SIZE = -1")
			execSQL("UPDATE $SCANS SET $SCANS_SEQUENCE_INDEX = -1")
			execSQL("UPDATE $SCANS SET $SCANS_GTIN_COUNTRY = $SCANS_POSSIBLE_COUNTRY")
			execSQL("UPDATE $SCANS SET $SCANS_GTIN_ADD_ON = $SCANS_UPC_EAN_EXTENSION")
			execSQL("UPDATE $SCANS SET $SCANS_GTIN_PRICE = $SCANS_SUGGESTED_PRICE")
			execSQL("UPDATE $SCANS SET $SCANS_GTIN_ISSUE_NUMBER = $SCANS_ISSUE_NUMBER")
		}

		private fun SQLiteDatabase.migrateToVersionString() {
			execSQL("ALTER TABLE $SCANS ADD $SCANS_VERSION TEXT")
			execSQL("UPDATE $SCANS SET $SCANS_VERSION = $SCANS_VERSION_NUMBER")
		}
	}
}
