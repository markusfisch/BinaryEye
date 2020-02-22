package de.markusfisch.android.binaryeye.app

import android.app.Application
import android.support.v8.renderscript.RenderScript
import de.markusfisch.android.binaryeye.BuildConfig
import de.markusfisch.android.binaryeye.preference.Preferences
import de.markusfisch.android.binaryeye.repository.DatabaseRepository

val db = DatabaseRepository()
val prefs = Preferences()

class BinaryEyeApp : Application() {
	override fun onCreate() {
		super.onCreate()

		// Since RenderScript can no longer be compiled on macOS Catalina
		// because Catalina cannot run 32bit binaries and Google failed
		// to provide a 64bit compiler in time. Now, as if that wasn't
		// bad enough, `RenderScript.forceCompat()` makes any build compiled
		// with the new build tools that provide a 64bit compiler crash
		// so we need to exclude this if we're building on Catalina.
		// Maybe `RenderScript.forceCompat()` isn't required anymore for
		// those new builds but I don't have a device to test this.
		if (!BuildConfig.IS_CATALINA) {
			// required to make RenderScript work for Lineage 16.0
			// possibly because of a system/device bug
			RenderScript.forceCompat()
		}

		db.open(this)
		prefs.init(this)
	}
}
