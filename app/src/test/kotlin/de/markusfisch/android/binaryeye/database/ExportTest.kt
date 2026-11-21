package de.markusfisch.android.binaryeye.database

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ExportTest {
	@get:Rule
	val files = TemporaryFolder()

	@Test
	fun snapshotIncludesCommittedWalFrames() {
		val source = files.newFile("history.db")
		val snapshot = files.newFile("snapshot.db")
		SQLiteDatabase.openOrCreateDatabase(source, null).use { db ->
			assertTrue(db.enableWriteAheadLogging())
			db.version = 10
			db.execSQL("CREATE TABLE scans (_id INTEGER PRIMARY KEY AUTOINCREMENT, content BLOB)")
			db.execSQL("INSERT INTO scans (content) VALUES (x'00ff')")
			db.execSQL("INSERT INTO scans (_id) VALUES (9)")
			db.execSQL("DELETE FROM scans WHERE _id = 9")
			assertTrue(File("${source.path}-wal").length() > 32)

			copyDatabaseSnapshot(db.path, snapshot)
			db.execSQL("INSERT INTO scans (content) VALUES ('later')")
		}

		val standalone = snapshot.copyTo(File(files.root, "export.db"))
		SQLiteDatabase.openDatabase(
			standalone.path, null, SQLiteDatabase.OPEN_READONLY
		).use { db ->
			assertEquals(10, db.version)
			db.rawQuery("SELECT content FROM scans", null).use { cursor ->
				assertEquals(1, cursor.count)
				assertTrue(cursor.moveToFirst())
				assertArrayEquals(byteArrayOf(0, -1), cursor.getBlob(0))
			}
			db.rawQuery("SELECT seq FROM sqlite_sequence WHERE name = 'scans'", null).use {
				assertTrue(it.moveToFirst())
				assertEquals(9, it.getInt(0))
			}
		}
	}
}
