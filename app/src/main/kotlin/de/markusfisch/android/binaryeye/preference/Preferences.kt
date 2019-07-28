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
	var openWithUrl: String = ""
		set(value) {
			setString(OPEN_WITH_URL, value)
			field = value
		}

	fun init(context: Context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context)
		update()
	}

	fun update() {
		useHistory = preferences.getBoolean(USE_HISTORY, useHistory)
		openWithUrl = preferences.getString(OPEN_WITH_URL, openWithUrl)
	}

	private fun setBoolean(label: String, value: Boolean) {
		val editor = preferences.edit()
		editor.putBoolean(label, value)
		editor.apply()
	}

	private fun setString(label: String, value: String) {
		val editor = preferences.edit()
		editor.putString(label, value)
		editor.apply()
	}

	companion object {
		const val USE_HISTORY = "use_history"
		const val OPEN_WITH_URL = "open_with_url"
	}
}
