package de.markusfisch.android.binaryeye.database

import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import de.markusfisch.android.binaryeye.database.Database.Companion.SCANS
import java.io.File

internal fun copyDatabaseSnapshot(sourcePath: String, destination: File) {
	// An error in an attached database must never let Android delete the live history.
	SQLiteDatabase.openDatabase(
		destination.path,
		null,
		SQLiteDatabase.CREATE_IF_NECESSARY,
		{}
	).use { snapshot ->
		snapshot.execSQL("ATTACH DATABASE ? AS source", arrayOf(sourcePath))
		snapshot.beginTransactionNonExclusive()
		try {
			val schema = DatabaseUtils.stringForQuery(
				snapshot,
				"SELECT sql FROM source.sqlite_master WHERE type = 'table' AND name = ?",
				arrayOf(SCANS)
			)
			snapshot.execSQL(schema)
			snapshot.execSQL("INSERT INTO main.$SCANS SELECT * FROM source.$SCANS")
			snapshot.execSQL("DELETE FROM main.sqlite_sequence WHERE name = ?", arrayOf(SCANS))
			snapshot.execSQL(
				"INSERT INTO main.sqlite_sequence SELECT * FROM source.sqlite_sequence WHERE name = ?",
				arrayOf(SCANS)
			)
			snapshot.version = DatabaseUtils.longForQuery(
				snapshot, "PRAGMA source.user_version", null
			).toInt()
			snapshot.setTransactionSuccessful()
		} finally {
			snapshot.endTransaction()
		}
	}
}
