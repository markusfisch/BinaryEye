package de.markusfisch.android.binaryeye.preference

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Preferences {
	lateinit var preferences: SharedPreferences

	var showCropHandle = true
		set(value) {
			apply(SHOW_CROP_HANDLE, value)
			field = value
		}
	var zoomBySwiping = true
		set(value) {
			apply(ZOOM_BY_SWIPING, value)
			field = value
		}
	var autoRotate = false
		set(value) {
			apply(AUTO_ROTATE, value)
			field = value
		}
	var tryHarder = false
		set(value) {
			apply(TRY_HARDER, value)
			field = value
		}
	var vibrate = true
		set(value) {
			apply(VIBRATE, value)
			field = value
		}
	var useHistory = false
		set(value) {
			apply(USE_HISTORY, value)
			field = value
		}
	var ignoreConsecutiveDuplicates = true
		set(value) {
			apply(IGNORE_CONSECUTIVE_DUPLICATES, value)
			field = value
		}
	var openImmediately = false
		set(value) {
			apply(OPEN_IMMEDIATELY, value)
			field = value
		}
	var showMetaData = true
		set(value) {
			apply(SHOW_META_DATA, value)
			field = value
		}
	var showHexDump = true
		set(value) {
			apply(SHOW_HEX_DUMP, value)
			field = value
		}
	var openWithUrl: String = ""
		set(value) {
			apply(OPEN_WITH_URL, value)
			field = value
		}
	var indexOfLastSelectedFormat: Int = 0
		set(value) {
			apply(INDEX_OF_LAST_SELECTED_FORMAT, value)
			field = value
		}
	var forceCompat: Boolean = false
		set(value) {
			// since the app may be about to crash when forceCompat is set,
			// it's necessary to `commit()` this synchronously
			commit(FORCE_COMPAT, value)
			field = value
		}

	fun init(context: Context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context)
		update()
	}

	fun update() {
		showCropHandle = preferences.getBoolean(SHOW_CROP_HANDLE, showCropHandle)
		zoomBySwiping = preferences.getBoolean(ZOOM_BY_SWIPING, zoomBySwiping)
		autoRotate = preferences.getBoolean(AUTO_ROTATE, autoRotate)
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

	private fun put(label: String, value: Boolean) =
		preferences.edit().putBoolean(label, value)

	private fun commit(label: String, value: Boolean) {
		put(label, value).commit()
	}

	private fun apply(label: String, value: Boolean) {
		put(label, value).apply()
	}

	private fun apply(label: String, value: String) {
		preferences.edit().putString(label, value).apply()
	}

	private fun apply(label: String, value: Int) {
		preferences.edit().putInt(label, value).apply()
	}

	companion object {
		const val SHOW_CROP_HANDLE = "show_crop_handle"
		const val ZOOM_BY_SWIPING = "zoom_by_swiping"
		const val AUTO_ROTATE = "auto_rotate"
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
