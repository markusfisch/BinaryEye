package de.markusfisch.android.binaryeye.preference

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Preferences {
	lateinit var preferences: SharedPreferences

	var cropHandleX = -2 // -2 means set default roi.
		set(value) {
			apply(CROP_HANDLE_X, value)
			field = value
		}
	var cropHandleY = -2
		set(value) {
			apply(CROP_HANDLE_Y, value)
			field = value
		}
	var cropHandleOrientation = 0
		set(value) {
			apply(CROP_HANDLE_ORIENTATION, value)
			field = value
		}
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
			// Immediately save this setting before it shouldn't change
			// on the fly while scanning.
			commit(AUTO_ROTATE, value)
			field = value
		}
	var tryHarder = false
		set(value) {
			// Immediately save this setting because it's only ever read
			// before the camera is opened.
			commit(TRY_HARDER, value)
			field = value
		}
	var bulkMode = false
		set(value) {
			// Immediately save this setting because it's only ever read
			// before the camera is opened.
			commit(BULK_MODE, value)
			field = value
		}
	var showToastInBulkMode = true
		set(value) {
			// Immediately save this setting because it's only ever read
			// before the camera is opened.
			commit(SHOW_TOAST_IN_BULK_MODE, value)
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
	var copyImmediately = false
		set(value) {
			apply(COPY_IMMEDIATELY, value)
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
	var closeAutomatically = false
		set(value) {
			apply(CLOSE_AUTOMATICALLY, value)
			field = value
		}
	var openWithUrl: String = ""
		set(value) {
			apply(OPEN_WITH_URL, value)
			field = value
		}
	var sendScanUrl: String = ""
		set(value) {
			apply(SEND_SCAN_URL, value)
			field = value
		}
	var sendScanType: String = "0"
		set(value) {
			apply(SEND_SCAN_TYPE, value)
			field = value
		}
	var customLocale: String = ""
		set(value) {
			apply(CUSTOM_LOCALE, value)
			field = value
		}
	var indexOfLastSelectedFormat: Int = 0
		set(value) {
			apply(INDEX_OF_LAST_SELECTED_FORMAT, value)
			field = value
		}
	var forceCompat: Boolean = false
		set(value) {
			// Since the app may be about to crash when forceCompat is set,
			// it's necessary to `commit()` this synchronously.
			commit(FORCE_COMPAT, value)
			field = value
		}

	fun init(context: Context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context)
		update()
	}

	fun update() {
		cropHandleX = preferences.getInt(CROP_HANDLE_X, cropHandleX)
		cropHandleY = preferences.getInt(CROP_HANDLE_Y, cropHandleY)
		cropHandleOrientation = preferences.getInt(
			CROP_HANDLE_ORIENTATION,
			cropHandleOrientation
		)
		showCropHandle = preferences.getBoolean(
			SHOW_CROP_HANDLE,
			showCropHandle
		)
		zoomBySwiping = preferences.getBoolean(ZOOM_BY_SWIPING, zoomBySwiping)
		autoRotate = preferences.getBoolean(AUTO_ROTATE, autoRotate)
		tryHarder = preferences.getBoolean(TRY_HARDER, tryHarder)
		bulkMode = preferences.getBoolean(BULK_MODE, bulkMode)
		showToastInBulkMode = preferences.getBoolean(
			SHOW_TOAST_IN_BULK_MODE,
			showToastInBulkMode
		)
		vibrate = preferences.getBoolean(VIBRATE, vibrate)
		useHistory = preferences.getBoolean(USE_HISTORY, useHistory)
		ignoreConsecutiveDuplicates = preferences.getBoolean(
			IGNORE_CONSECUTIVE_DUPLICATES,
			ignoreConsecutiveDuplicates
		)
		copyImmediately = preferences.getBoolean(
			COPY_IMMEDIATELY,
			copyImmediately
		)
		openImmediately = preferences.getBoolean(
			OPEN_IMMEDIATELY,
			openImmediately
		)
		showMetaData = preferences.getBoolean(SHOW_META_DATA, showMetaData)
		showHexDump = preferences.getBoolean(SHOW_HEX_DUMP, showHexDump)
		closeAutomatically = preferences.getBoolean(
			CLOSE_AUTOMATICALLY,
			closeAutomatically
		)
		preferences.getString(OPEN_WITH_URL, openWithUrl)?.also {
			openWithUrl = it
		}
		preferences.getString(SEND_SCAN_URL, sendScanUrl)?.also {
			sendScanUrl = it
		}
		preferences.getString(SEND_SCAN_TYPE, sendScanType)?.also {
			sendScanType = it
		}
		preferences.getString(CUSTOM_LOCALE, customLocale)?.also {
			customLocale = it
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
		const val CROP_HANDLE_X = "crop_handle_x"
		const val CROP_HANDLE_Y = "crop_handle_y"
		const val CROP_HANDLE_ORIENTATION = "crop_handle_orientation"
		const val SHOW_CROP_HANDLE = "show_crop_handle"
		const val ZOOM_BY_SWIPING = "zoom_by_swiping"
		const val AUTO_ROTATE = "auto_rotate"
		const val TRY_HARDER = "try_harder"
		const val BULK_MODE = "bulk_mode"
		const val SHOW_TOAST_IN_BULK_MODE = "show_toast_in_bulk_mode"
		const val VIBRATE = "vibrate"
		const val USE_HISTORY = "use_history"
		const val IGNORE_CONSECUTIVE_DUPLICATES = "ignore_consecutive_duplicates"
		const val OPEN_IMMEDIATELY = "open_immediately"
		const val COPY_IMMEDIATELY = "copy_immediately"
		const val SHOW_META_DATA = "show_meta_data"
		const val SHOW_HEX_DUMP = "show_hex_dump"
		const val CLOSE_AUTOMATICALLY = "close_automatically"
		const val OPEN_WITH_URL = "open_with_url"
		const val SEND_SCAN_URL = "send_scan_url"
		const val SEND_SCAN_TYPE = "send_scan_type"
		const val CUSTOM_LOCALE = "custom_locale"
		const val INDEX_OF_LAST_SELECTED_FORMAT = "index_of_last_selected_format"
		const val FORCE_COMPAT = "force_compat"
	}
}
