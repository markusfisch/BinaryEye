package de.markusfisch.android.binaryeye.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Preferences {
	lateinit var preferences: SharedPreferences

	var zoomBySwiping = true
		set(value) {
			setBoolean(ZOOM_BY_SWIPING, value)
			field = value
		}
	var tryHarder = false
		set(value) {
			setBoolean(TRY_HARDER, value)
			field = value
		}
	var vibrate = true
		set(value) {
			setBoolean(VIBRATE, value)
			field = value
		}
	var useHistory = false
		set(value) {
			setBoolean(USE_HISTORY, value)
			field = value
		}
	var ignoreConsecutiveDuplicates = true
		set(value) {
			setBoolean(IGNORE_CONSECUTIVE_DUPLICATES, value)
			field = value
		}
	var openImmediately = false
		set(value) {
			setBoolean(OPEN_IMMEDIATELY, value)
			field = value
		}
	var showMetaData = true
		set(value) {
			setBoolean(SHOW_META_DATA, value)
			field = value
		}
	var showHexDump = true
		set(value) {
			setBoolean(SHOW_HEX_DUMP, value)
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
	var forceCompat: Boolean = false
		@SuppressLint("ApplySharedPref")
		set(value) {
			// since the app may be about to crash when forceCompat is set,
			// it's necessary to `commit()` this synchronously
			preferences.edit().putBoolean(FORCE_COMPAT, value).commit()
			field = value
		}

	fun init(context: Context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context)
		update()
	}

	fun update() {
		zoomBySwiping = preferences.getBoolean(ZOOM_BY_SWIPING, zoomBySwiping)
		tryHarder = preferences.getBoolean(TRY_HARDER, tryHarder)
		vibrate = preferences.getBoolean(VIBRATE, vibrate)
		useHistory = preferences.getBoolean(USE_HISTORY, useHistory)
		ignoreConsecutiveDuplicates = preferences.getBoolean(
			IGNORE_CONSECUTIVE_DUPLICATES,
			ignoreConsecutiveDuplicates
		)
		openImmediately = preferences.getBoolean(
			OPEN_IMMEDIATELY,
			openImmediately
		)
		showMetaData = preferences.getBoolean(SHOW_META_DATA, showMetaData)
		showHexDump = preferences.getBoolean(SHOW_HEX_DUMP, showHexDump)
		preferences.getString(OPEN_WITH_URL, openWithUrl)?.also {
			openWithUrl = it
		}
		indexOfLastSelectedFormat = preferences.getInt(
			INDEX_OF_LAST_SELECTED_FORMAT,
			indexOfLastSelectedFormat
		)
		forceCompat = preferences.getBoolean(FORCE_COMPAT, forceCompat)
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
		const val ZOOM_BY_SWIPING = "zoom_by_swiping"
		const val TRY_HARDER = "try_harder"
		const val VIBRATE = "vibrate"
		const val USE_HISTORY = "use_history"
		const val IGNORE_CONSECUTIVE_DUPLICATES = "ignore_consecutive_duplicates"
		const val OPEN_IMMEDIATELY = "open_immediately"
		const val SHOW_HEX_DUMP = "show_hex_dump"
		const val SHOW_META_DATA = "show_meta_data"
		const val OPEN_WITH_URL = "open_with_url"
		const val INDEX_OF_LAST_SELECTED_FORMAT = "index_of_last_selected_format"
		const val FORCE_COMPAT = "force_compat"
	}
}
