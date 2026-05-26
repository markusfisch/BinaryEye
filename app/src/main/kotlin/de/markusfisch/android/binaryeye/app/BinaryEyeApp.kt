package de.markusfisch.android.binaryeye.app

import android.app.Application
import com.google.android.material.color.DynamicColors
import de.markusfisch.android.binaryeye.database.Database
import de.markusfisch.android.binaryeye.preference.Preferences

val db = Database()
val prefs = Preferences()

class BinaryEyeApp : Application() {
	override fun onCreate() {
		super.onCreate()
		prefs.init(this)
		DynamicColors.applyToActivitiesIfAvailable(
			this,
			DynamicColors.Precondition { _, _ -> prefs.dynamicColors }
		)
		db.open(this)
	}
}
