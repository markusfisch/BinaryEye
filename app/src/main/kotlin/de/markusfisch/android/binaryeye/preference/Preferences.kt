package de.markusfisch.android.binaryeye.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.media.ToneGenerator
import android.os.Build
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import de.markusfisch.android.binaryeye.automation.AutomatedAction
import de.markusfisch.android.binaryeye.automation.AutomatedAction.Companion.fromJsonArray
import de.markusfisch.android.binaryeye.automation.AutomatedAction.Companion.toJsonArray
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import org.json.JSONArray

class Preferences {
	lateinit var defaultPreferences: SharedPreferences
	lateinit var preferences: SharedPreferences

	val profiles = ArrayList<String>()

	var profile: String? = null
		set(value) {
			defaultPreferences.edit().putString(PROFILE, value).apply()
			field = value
		}
	var barcodeFormats = setOf(
		BarcodeFormat.AZTEC.name,
		BarcodeFormat.CODABAR.name,
		BarcodeFormat.CODE_39.name,
		BarcodeFormat.CODE_93.name,
		BarcodeFormat.CODE_128.name,
		BarcodeFormat.DATA_BAR.name,
		BarcodeFormat.DATA_BAR_EXPANDED.name,
		BarcodeFormat.DATA_MATRIX.name,
		BarcodeFormat.DX_FILM_EDGE.name,
		BarcodeFormat.EAN_8.name,
		BarcodeFormat.EAN_13.name,
		BarcodeFormat.ITF.name,
		BarcodeFormat.MAXICODE.name,
		BarcodeFormat.PDF_417.name,
		BarcodeFormat.QR_CODE.name,
		BarcodeFormat.MICRO_QR_CODE.name,
		BarcodeFormat.RMQR_CODE.name,
		BarcodeFormat.UPC_A.name,
		BarcodeFormat.UPC_E.name,
	)
		@RequiresApi(Build.VERSION_CODES.HONEYCOMB)
		set(value) {
			apply(BARCODE_FORMATS, value)
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
	var autoRotate = true
		set(value) {
			apply(AUTO_ROTATE, value)
			field = value
		}
	var tryHarder = false
		set(value) {
			apply(TRY_HARDER, value)
			field = value
		}
	var bulkMode = false
		set(value) {
			apply(BULK_MODE, value)
			field = value
		}
	var bulkModeDelay = "500"
		set(value) {
			apply(BULK_MODE_DELAY, value)
			field = value
		}
	var showToastInBulkMode = true
		set(value) {
			apply(SHOW_TOAST_IN_BULK_MODE, value)
			field = value
		}
	var vibrate = true
		set(value) {
			apply(VIBRATE, value)
			field = value
		}
	var beep = false
		set(value) {
			apply(BEEP, value)
			field = value
		}
	var beepToneName = "tone_prop_beep"
		set(value) {
			apply(BEEP_TONE_NAME, value)
			field = value
		}
	var useHistory = false
		set(value) {
			apply(USE_HISTORY, value)
			field = value
		}
	var ignoreDuplicatesName = "ignore_consecutive_duplicates"
		set(value) {
			apply(IGNORE_DUPLICATES_NAME, value)
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
	var showChecksum = ""
		set(value) {
			apply(SHOW_CHECKSUM, value)
			field = value
		}
	var showRecreation = true
		set(value) {
			apply(SHOW_RECREATION, value)
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
	var sendScanActive = false
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
	var sendScanBluetooth = false
		set(value) {
			apply(SEND_SCAN_BLUETOOTH, value)
			field = value
		}
	var sendScanBluetoothHost: String = ""
		set(value) {
			apply(SEND_SCAN_BLUETOOTH_HOST, value)
			field = value
		}
	var customLocale: String = ""
		set(value) {
			// Make sure this setting is written immediately because
			// the app is about to restart.
			commit(CUSTOM_LOCALE, value)
			field = value
		}
	var indexOfLastSelectedFormat: Int = 0
		set(value) {
			apply(INDEX_OF_LAST_SELECTED_FORMAT, value)
			field = value
		}
	var indexOfLastSelectedEcLevel: Int = 0
		set(value) {
			apply(INDEX_OF_LAST_SELECTED_EC_LEVEL, value)
			field = value
		}
	var freeRotation = false
		set(value) {
			apply(FREE_ROTATION, value)
			field = value
		}
	var expandEscapeSequences = true
		set(value) {
			apply(EXPAND_ESCAPE_SEQUENCES, value)
			field = value
		}
	var addQuietZone = true
		set(value) {
			apply(ADD_QUIET_ZONE, value)
			field = value
		}
	var brightenScreen = false
		set(value) {
			apply(BRIGHTEN_SCREEN, value)
			field = value
		}
	var previewScale = 0f
		set(value) {
			apply(PREVIEW_SCALE, value)
			field = value
		}
	var automatedActions = mutableListOf<AutomatedAction>()
		private set

	fun init(context: Context) {
		// I'm not including a support library just to get the
		// default preferences. Dependencies are a burden.
		@Suppress("DEPRECATION")
		defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context)

		loadProfiles()
		load(context, defaultPreferences.getString(PROFILE, null))
	}

	fun load(context: Context, profileName: String?) {
		preferences = if (profileName.isNullOrEmpty()) {
			profile = null
			defaultPreferences
		} else {
			profile = profileName
			context.getSharedPreferences(
				profileName,
				Context.MODE_PRIVATE
			)
		}
		update()
	}

	fun addProfile(name: String): Boolean {
		if (!name.matches("^[a-zA-Z0-9_-]+$".toRegex()) ||
			profiles.contains(name)
		) {
			return false
		}
		profiles.add(name)
		saveProfiles()
		return true
	}

	fun removeProfile(name: String): Boolean {
		if (!profiles.remove(name)) {
			return false
		}
		saveProfiles()
		return true
	}

	fun saveProfiles() {
		defaultPreferences.edit().putString(
			PROFILES,
			JSONArray(profiles).toString()
		).apply()
	}

	fun loadProfiles() {
		profiles.clear()
		JSONArray(
			defaultPreferences.getString(PROFILES, "[]")
		).let {
			profiles.addAll(Array(it.length()) { i -> it.getString(i) })
		}
	}

	fun update() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			preferences.getStringSet(BARCODE_FORMATS, barcodeFormats)?.let {
				barcodeFormats = addFormatsOnUpdate(
					it,
					BarcodeFormat.RMQR_CODE,
					BarcodeFormat.DX_FILM_EDGE
				)
			}
		}

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
		beep = preferences.getBoolean(BEEP, beep)
		preferences.getString(BEEP_TONE_NAME, beepToneName)?.also {
			beepToneName = it
		}
		useHistory = preferences.getBoolean(USE_HISTORY, useHistory)

		// Map old setting to new one if it wasn't on default value.
		val ignoreConsecutiveDuplicates = "ignore_consecutive_duplicates"
		if (!preferences.getBoolean(ignoreConsecutiveDuplicates, true)) {
			preferences.edit().remove(ignoreConsecutiveDuplicates).apply()
			ignoreDuplicatesName = "accept_duplicates"
		}

		preferences.getString(
			IGNORE_DUPLICATES_NAME,
			ignoreDuplicatesName
		)?.also {
			ignoreDuplicatesName = it
		}
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
		preferences.getString(SHOW_CHECKSUM, showChecksum)?.also {
			showChecksum = it
		}
		showRecreation = preferences.getBoolean(SHOW_RECREATION, showRecreation)
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
		sendScanBluetooth = preferences.getBoolean(
			SEND_SCAN_BLUETOOTH,
			sendScanBluetooth
		)
		preferences.getString(
			SEND_SCAN_BLUETOOTH_HOST,
			sendScanBluetoothHost
		)?.also {
			sendScanBluetoothHost = it
		}
		preferences.getString(CUSTOM_LOCALE, customLocale)?.also {
			customLocale = it
		}
		indexOfLastSelectedFormat = preferences.getInt(
			INDEX_OF_LAST_SELECTED_FORMAT,
			indexOfLastSelectedFormat
		)
		indexOfLastSelectedEcLevel = preferences.getInt(
			INDEX_OF_LAST_SELECTED_EC_LEVEL,
			indexOfLastSelectedEcLevel
		)
		freeRotation = preferences.getBoolean(FREE_ROTATION, freeRotation)
		expandEscapeSequences = preferences.getBoolean(
			EXPAND_ESCAPE_SEQUENCES,
			expandEscapeSequences
		)
		addQuietZone = preferences.getBoolean(ADD_QUIET_ZONE, addQuietZone)
		brightenScreen = preferences.getBoolean(
			BRIGHTEN_SCREEN,
			brightenScreen
		)
		previewScale = preferences.getFloat(
			PREVIEW_SCALE,
			previewScale
		)
		automatedActions = fromJsonArray(
			preferences.getString(AUTOMATED_ACTIONS, "[]") ?: "[]"
		)
	}

	fun setAutomatedActions(actions: List<AutomatedAction>) {
		automatedActions = actions.toMutableList()
		apply(AUTOMATED_ACTIONS, toJsonArray(actions))
	}

	private fun addFormatsOnUpdate(
		restored: Set<String>,
		vararg formats: BarcodeFormat
	) = restored.toMutableSet().apply {
		for (format in formats) {
			val name = "${format.name}_added"
			if (!preferences.getBoolean(name, false)) {
				preferences.edit().putBoolean(name, true).apply()
				add(format.name)
			}
		}
	}

	fun restoreCropHandle(
		name: String,
		viewWidth: Int,
		viewHeight: Int
	) = preferences.restoreCropHandle(name, viewWidth, viewHeight)

	fun storeCropHandle(
		name: String,
		viewWidth: Int,
		viewHeight: Int,
		cropHandle: Point
	) = preferences.storeCropHandle(name, viewWidth, viewHeight, cropHandle)

	fun beepTone() = when (beepToneName) {
		"tone_cdma_confirm" -> ToneGenerator.TONE_CDMA_CONFIRM
		"tone_sup_radio_ack" -> ToneGenerator.TONE_SUP_RADIO_ACK
		"tone_prop_ack" -> ToneGenerator.TONE_PROP_ACK
		"tone_prop_beep" -> ToneGenerator.TONE_PROP_BEEP
		"tone_prop_beep2" -> ToneGenerator.TONE_PROP_BEEP2
		else -> ToneGenerator.TONE_PROP_BEEP
	}

	fun ignoreDuplicates() = when (ignoreDuplicatesName) {
		"ignore_consecutive_duplicates" -> IgnoreDuplicates.Consecutive
		"ignore_any_duplicates" -> IgnoreDuplicates.Any
		else -> IgnoreDuplicates.Never
	}

	private fun put(label: String, value: Boolean) =
		preferences.edit().putBoolean(label, value)

	private fun apply(label: String, value: Boolean) {
		put(label, value).apply()
	}

	private fun apply(label: String, value: String) {
		preferences.edit().putString(label, value).apply()
	}

	private fun apply(label: String, value: Int) {
		preferences.edit().putInt(label, value).apply()
	}

	private fun apply(label: String, value: Float) {
		preferences.edit().putFloat(label, value).apply()
	}

	@SuppressLint("ApplySharedPref")
	private fun commit(label: String, value: String) {
		preferences.edit().putString(label, value).commit()
	}

	@RequiresApi(Build.VERSION_CODES.HONEYCOMB)
	private fun apply(label: String, value: Set<String>) {
		preferences.edit().putStringSet(label, value).apply()
	}

	companion object {
		enum class IgnoreDuplicates {
			Consecutive, Any, Never
		}

		private const val PROFILES = "profiles"
		private const val PROFILE = "profile"
		private const val BARCODE_FORMATS = "formats"
		private const val SHOW_CROP_HANDLE = "show_crop_handle"
		private const val ZOOM_BY_SWIPING = "zoom_by_swiping"
		private const val AUTO_ROTATE = "auto_rotate"
		private const val TRY_HARDER = "try_harder"
		private const val BULK_MODE = "bulk_mode"
		private const val BULK_MODE_DELAY = "bulk_mode_delay"
		private const val SHOW_TOAST_IN_BULK_MODE = "show_toast_in_bulk_mode"
		private const val VIBRATE = "vibrate"
		private const val BEEP = "beep"
		private const val BEEP_TONE_NAME = "beep_tone_name"
		private const val USE_HISTORY = "use_history"
		private const val IGNORE_DUPLICATES_NAME = "ignore_duplicates_name"
		private const val OPEN_IMMEDIATELY = "open_immediately"
		private const val COPY_IMMEDIATELY = "copy_immediately"
		private const val SHOW_META_DATA = "show_meta_data"
		private const val SHOW_HEX_DUMP = "show_hex_dump"
		private const val SHOW_CHECKSUM = "show_checksum"
		private const val SHOW_RECREATION = "show_recreation"
		private const val CLOSE_AUTOMATICALLY = "close_automatically"
		private const val DEFAULT_SEARCH_URL = "default_search_url"
		private const val OPEN_WITH_URL = "open_with_url"
		private const val SEND_SCAN_ACTIVE = "send_scan_active"
		private const val SEND_SCAN_URL = "send_scan_url"
		private const val SEND_SCAN_TYPE = "send_scan_type"
		private const val SEND_SCAN_BLUETOOTH = "send_scan_bluetooth"
		private const val SEND_SCAN_BLUETOOTH_HOST = "send_scan_bluetooth_host"
		private const val CUSTOM_LOCALE = "custom_locale"
		private const val INDEX_OF_LAST_SELECTED_FORMAT = "index_of_last_selected_format"
		private const val INDEX_OF_LAST_SELECTED_EC_LEVEL = "index_of_last_selected_ec_level"
		private const val FREE_ROTATION = "free_rotation"
		private const val EXPAND_ESCAPE_SEQUENCES = "expand_escape_sequences"
		private const val ADD_QUIET_ZONE = "add_quiet_zone"
		private const val BRIGHTEN_SCREEN = "brighten_screen"
		private const val PREVIEW_SCALE = "preview_scale"
		private const val AUTOMATED_ACTIONS = "automated_actions"
	}
}
