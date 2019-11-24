package de.markusfisch.android.binaryeye.preference

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Preferences {
	lateinit var preferences: SharedPreferences

	var openImmediately = false
		set(value) {
			setBoolean(OPEN_IMMEDIATELY, value)
			field = value
		}
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
	var indexOfLastSelectedFormat: Int = 0
		set(value) {
			setInt(INDEX_OF_LAST_SELECTED_FORMAT, value)
			field = value
		}

	fun init(context: Context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context)
		update()
	}

	fun update() {
		openImmediately = preferences.getBoolean(
			OPEN_IMMEDIATELY,
			openImmediately
		)
		useHistory = preferences.getBoolean(USE_HISTORY, useHistory)
		indexOfLastSelectedFormat = preferences.getInt(
			INDEX_OF_LAST_SELECTED_FORMAT,
			indexOfLastSelectedFormat
		)
		preferences.getString(OPEN_WITH_URL, openWithUrl)?.also {
			openWithUrl = it
		}
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

	private fun setInt(label: String, value: Int) {
		val editor = preferences.edit()
		editor.putInt(label, value)
		editor.apply()
	}

	companion object {
		const val OPEN_IMMEDIATELY = "open_immediately"
		const val USE_HISTORY = "use_history"
		const val OPEN_WITH_URL = "open_with_url"
		const val INDEX_OF_LAST_SELECTED_FORMAT = "index_of_last_selected_format"
	}
}
