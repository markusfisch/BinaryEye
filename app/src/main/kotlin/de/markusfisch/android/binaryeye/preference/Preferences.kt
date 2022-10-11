package de.markusfisch.android.binaryeye.preference

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import de.markusfisch.android.zxingcpp.ZxingCpp.Format

class Preferences {
	lateinit var preferences: SharedPreferences

	var barcodeFormats = setOf(
		Format.AZTEC.name,
		Format.CODABAR.name,
		Format.CODE_39.name,
		Format.CODE_93.name,
		Format.CODE_128.name,
		Format.DATA_BAR.name,
		Format.DATA_BAR_EXPANDED.name,
		Format.DATA_MATRIX.name,
		Format.EAN_8.name,
		Format.EAN_13.name,
		Format.ITF.name,
		Format.MAXICODE.name,
		Format.PDF_417.name,
		Format.QR_CODE.name,
		Format.MICRO_QR_CODE.name,
		Format.UPC_A.name,
		Format.UPC_E.name,
	)
		@RequiresApi(Build.VERSION_CODES.HONEYCOMB)
		set(value) {
			apply(BARCODE_FORMATS, value)
			field = value
		}
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
	var bulkModeDelay = "500"
		set(value) {
			// Immediately save this setting because it's only ever read
			// before the camera is opened.
			commit(BULK_MODE_DELAY, value)
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
	var defaultSearchUrl = ""
		set(value) {
			apply(DEFAULT_SEARCH_URL, value)
			field = value
		}
	var openWithUrl: String = ""
		set(value) {
			apply(OPEN_WITH_URL, value)
			field = value
		}
	var sendScanActive = true
		set(value) {
			apply(SEND_SCAN_ACTIVE, value)
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
	var forceCompat = false
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			preferences.getStringSet(BARCODE_FORMATS, barcodeFormats)?.let {
				barcodeFormats = it
			}
		}
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
		bulkModeDelay = preferences.getString(
			BULK_MODE_DELAY,
			bulkModeDelay
		) ?: bulkModeDelay
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
		preferences.getString(DEFAULT_SEARCH_URL, defaultSearchUrl)?.also {
			defaultSearchUrl = it
		}
		preferences.getString(OPEN_WITH_URL, openWithUrl)?.also {
			openWithUrl = it
		}
		sendScanActive = preferences.getBoolean(
			SEND_SCAN_ACTIVE,
			sendScanActive
		)
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

	private fun put(label: String, value: String) =
		preferences.edit().putString(label, value)

	private fun commit(label: String, value: Boolean) {
		put(label, value).commit()
	}

	private fun commit(label: String, value: String) {
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

	@RequiresApi(Build.VERSION_CODES.HONEYCOMB)
	private fun apply(label: String, value: Set<String>) {
		preferences.edit().putStringSet(label, value).apply()
	}

	companion object {
		private const val BARCODE_FORMATS = "formats"
		private const val CROP_HANDLE_X = "crop_handle_x"
		private const val CROP_HANDLE_Y = "crop_handle_y"
		private const val CROP_HANDLE_ORIENTATION = "crop_handle_orientation"
		private const val SHOW_CROP_HANDLE = "show_crop_handle"
		private const val ZOOM_BY_SWIPING = "zoom_by_swiping"
		private const val AUTO_ROTATE = "auto_rotate"
		private const val TRY_HARDER = "try_harder"
		private const val BULK_MODE = "bulk_mode"
		private const val BULK_MODE_DELAY = "bulk_mode_delay"
		private const val SHOW_TOAST_IN_BULK_MODE = "show_toast_in_bulk_mode"
		private const val VIBRATE = "vibrate"
		private const val USE_HISTORY = "use_history"
		private const val IGNORE_CONSECUTIVE_DUPLICATES = "ignore_consecutive_duplicates"
		private const val OPEN_IMMEDIATELY = "open_immediately"
		private const val COPY_IMMEDIATELY = "copy_immediately"
		private const val SHOW_META_DATA = "show_meta_data"
		private const val SHOW_HEX_DUMP = "show_hex_dump"
		private const val CLOSE_AUTOMATICALLY = "close_automatically"
		private const val DEFAULT_SEARCH_URL = "default_search_url"
		private const val OPEN_WITH_URL = "open_with_url"
		private const val SEND_SCAN_ACTIVE = "send_scan_active"
		private const val SEND_SCAN_URL = "send_scan_url"
		private const val SEND_SCAN_TYPE = "send_scan_type"
		private const val CUSTOM_LOCALE = "custom_locale"
		private const val INDEX_OF_LAST_SELECTED_FORMAT = "index_of_last_selected_format"
		private const val FORCE_COMPAT = "force_compat"
	}
}
