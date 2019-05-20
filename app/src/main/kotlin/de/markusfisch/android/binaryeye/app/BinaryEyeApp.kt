package de.markusfisch.android.binaryeye.app

import de.markusfisch.android.binaryeye.data.Database
import de.markusfisch.android.binaryeye.preference.Preferences

import android.app.Application
import android.support.v8.renderscript.RenderScript

val db = Database()
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
