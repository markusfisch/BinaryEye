package de.markusfisch.android.binaryeye.app

import android.app.Application
import android.support.v8.renderscript.RenderScript
import de.markusfisch.android.binaryeye.preference.Preferences
import de.markusfisch.android.binaryeye.repository.DatabaseRepository

val db = DatabaseRepository()
val prefs = Preferences()

class BinaryEyeApp : Application() {
	override fun onCreate() {
		super.onCreate()

		// required to make RenderScript work for Lineage 16.0
		// possibly because of a system/device bug
		RenderScript.forceCompat()

		db.open(this)
		prefs.init(this)
	}
}
