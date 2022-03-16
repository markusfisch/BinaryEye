package de.markusfisch.android.binaryeye.app

import android.support.multidex.MultiDexApplication;
import android.support.v8.renderscript.RenderScript
import de.markusfisch.android.binaryeye.database.Database
import de.markusfisch.android.binaryeye.preference.Preferences

val db = Database()
val prefs = Preferences()

class BinaryEyeApp : MultiDexApplication() {
	override fun onCreate() {
		super.onCreate()
		db.open(this)
		prefs.init(this)

		if (prefs.forceCompat) {
			RenderScript.forceCompat()
		}
	}
}
