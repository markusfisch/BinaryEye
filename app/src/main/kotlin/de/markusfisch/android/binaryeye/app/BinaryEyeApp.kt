package de.markusfisch.android.binaryeye.app

import android.app.Application
import de.markusfisch.android.binaryeye.database.Database
import de.markusfisch.android.binaryeye.preference.Preferences

val db = Database()
val prefs = Preferences()

class BinaryEyeApp : Application() {
	override fun onCreate() {
		super.onCreate()
		db.open(this)
		prefs.init(this)
	}
}
