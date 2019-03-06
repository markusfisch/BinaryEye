package de.markusfisch.android.binaryeye.app

import de.markusfisch.android.binaryeye.data.Database
import de.markusfisch.android.binaryeye.preference.Preferences

import android.app.Application

val db = Database()
val prefs = Preferences()

class BinaryEyeApp : Application() {
	override fun onCreate() {
		super.onCreate()
		db.open(this)
		prefs.init(this)
	}
}
