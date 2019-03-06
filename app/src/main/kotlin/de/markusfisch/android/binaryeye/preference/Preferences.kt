package de.markusfisch.android.binaryeye.preference

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Preferences {
	lateinit var preferences: SharedPreferences

	var useHistory = false
		set(value) {
			setBoolean(USE_HISTORY, value)
			field = value
		}

	fun init(context: Context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context)
		update()
	}

	fun update() {
		useHistory = preferences.getBoolean(USE_HISTORY, useHistory)
	}

	private fun setBoolean(label: String, value: Boolean) {
		val editor = preferences.edit()
		editor.putBoolean(label, value)
		editor.apply()
	}

	companion object {
		const val USE_HISTORY = "use_history"
	}
}
