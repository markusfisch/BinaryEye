package de.markusfisch.android.binaryeye.app

import android.app.Application
import android.support.v8.renderscript.RenderScript
import de.markusfisch.android.binaryeye.data.Database
import de.markusfisch.android.binaryeye.preference.Preferences

val db = Database()
val prefs = Preferences()

class BinaryEyeApp : Application() {
	override fun onCreate() {
		super.onCreate()
		prefs.init(this)

		if (prefs.forceCompat) {
			RenderScript.forceCompat()
		}

		db.open(this)
	}
}
