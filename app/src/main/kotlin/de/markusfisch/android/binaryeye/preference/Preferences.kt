package de.markusfisch.android.binaryeye.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.media.ToneGenerator
import android.preference.PreferenceManager
import androidx.core.content.edit
import de.markusfisch.android.binaryeye.automation.AutomatedAction
import de.markusfisch.android.binaryeye.automation.AutomatedAction.Companion.fromJsonArray
import de.markusfisch.android.binaryeye.automation.AutomatedAction.Companion.toJsonArray
import de.markusfisch.android.binaryeye.zxingcpp.migrateBarcodeFormatName
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import de.markusfisch.android.binaryeye.preference.IgnoreCode.Companion.fromJsonArray as ignoreCodesFromJsonArray
import de.markusfisch.android.binaryeye.preference.IgnoreCode.Companion.toJsonArray as ignoreCodesToJsonArray

class Preferences {
	lateinit var defaultPreferences: SharedPreferences
	lateinit var preferences: SharedPreferences

	val profiles = ArrayList<String>()

	var profile: String? = null
		set(value) {
			defaultPreferences.edit {
				putString(PROFILE, value)
			}
			field = value
		}
	var barcodeFormats = setOf(
		BarcodeFormat.Aztec.name,
		BarcodeFormat.Codabar.name,
		BarcodeFormat.Code39Std.name,
		BarcodeFormat.Code39Ext.name,
		BarcodeFormat.Code32.name,
		BarcodeFormat.PZN.name,
		BarcodeFormat.Code93.name,
		BarcodeFormat.Code128.name,
		BarcodeFormat.DataBar.name,
		BarcodeFormat.DataBarOmni.name,
		BarcodeFormat.DataBarStk.name,
		BarcodeFormat.DataBarLtd.name,
		BarcodeFormat.DataBarExp.name,
		BarcodeFormat.DataBarExpStk.name,
		BarcodeFormat.DataMatrix.name,
		BarcodeFormat.DXFilmEdge.name,
		BarcodeFormat.EAN8.name,
		BarcodeFormat.EAN13.name,
		BarcodeFormat.ITF.name,
		BarcodeFormat.MaxiCode.name,
		BarcodeFormat.PDF417.name,
		BarcodeFormat.QRCode.name,
		BarcodeFormat.MicroQRCode.name,
		BarcodeFormat.RMQRCode.name,
		BarcodeFormat.UPCA.name,
		BarcodeFormat.UPCE.name,
	)
		set(value) {
			apply(BARCODE_FORMATS, value)
			field = value
		}
	var showCropHandle = true
		set(value) {
			apply(SHOW_CROP_HANDLE, value)
			field = value
		}
	var showCrosshairs = false
		set(value) {
			apply(SHOW_CROSSHAIRS, value)
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
	var beepStreamName = "stream_music"
		set(value) {
			apply(BEEP_STREAM_NAME, value)
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
	var ignoreCodes = mutableListOf(IgnoreCode(DEFAULT_IGNORE_CODE_PATTERN))
		private set
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
		defaultPreferences.edit {
			putString(PROFILES, org.json.JSONArray(profiles).toString())
		}
	}

	fun loadProfiles() {
		profiles.clear()
		org.json.JSONArray(
			defaultPreferences.getString(PROFILES, "[]")
		).let {
			profiles.addAll(Array(it.length()) { i -> it.getString(i) })
		}
	}

	fun toJson(context: Context, name: String?): String {
		val defaults = Preferences()
		val p = if (name.isNullOrEmpty()) {
			defaultPreferences
		} else {
			context.getSharedPreferences(name, Context.MODE_PRIVATE)
		}

		val json = JSONObject()
		json.put("type", PROFILE_EXPORT_TYPE)
		json.put("name", name ?: "")
		json.put(BARCODE_FORMATS, JSONArray().apply {
			(p.getStringSet(BARCODE_FORMATS, null) ?: defaults.barcodeFormats).forEach {
				put(it)
			}
		})
		json.put(SHOW_CROP_HANDLE, p, defaults.showCropHandle)
		json.put(SHOW_CROSSHAIRS, p, defaults.showCrosshairs)
		json.put(ZOOM_BY_SWIPING, p, defaults.zoomBySwiping)
		json.put(AUTO_ROTATE, p, defaults.autoRotate)
		json.put(TRY_HARDER, p, defaults.tryHarder)
		json.put(BULK_MODE, p, defaults.bulkMode)
		json.put(BULK_MODE_DELAY, p, defaults.bulkModeDelay)
		json.put(SHOW_TOAST_IN_BULK_MODE, p, defaults.showToastInBulkMode)
		json.put(VIBRATE, p, defaults.vibrate)
		json.put(BEEP, p, defaults.beep)
		json.put(BEEP_TONE_NAME, p, defaults.beepToneName)
		json.put(BEEP_STREAM_NAME, p, defaults.beepStreamName)
		json.put(USE_HISTORY, p, defaults.useHistory)
		json.put(IGNORE_DUPLICATES_NAME, p, defaults.ignoreDuplicatesName)
		json.put(IGNORE_CODES, p, ignoreCodesToJsonArray(defaults.ignoreCodes))
		json.put(COPY_IMMEDIATELY, p, defaults.copyImmediately)
		json.put(OPEN_IMMEDIATELY, p, defaults.openImmediately)
		json.put(SHOW_META_DATA, p, defaults.showMetaData)
		json.put(SHOW_HEX_DUMP, p, defaults.showHexDump)
		json.put(SHOW_CHECKSUM, p, defaults.showChecksum)
		json.put(SHOW_RECREATION, p, defaults.showRecreation)
		json.put(CLOSE_AUTOMATICALLY, p, defaults.closeAutomatically)
		json.put(DEFAULT_SEARCH_URL, p, defaults.defaultSearchUrl)
		json.put(OPEN_WITH_URL, p, defaults.openWithUrl)
		json.put(SEND_SCAN_ACTIVE, p, defaults.sendScanActive)
		json.put(SEND_SCAN_URL, p, defaults.sendScanUrl)
		json.put(SEND_SCAN_TYPE, p, defaults.sendScanType)
		json.put(SEND_SCAN_BLUETOOTH, p, defaults.sendScanBluetooth)
		json.put(SEND_SCAN_BLUETOOTH_HOST, p, defaults.sendScanBluetoothHost)
		json.put(CUSTOM_LOCALE, p, defaults.customLocale)
		json.put(INDEX_OF_LAST_SELECTED_FORMAT, p, defaults.indexOfLastSelectedFormat)
		json.put(INDEX_OF_LAST_SELECTED_EC_LEVEL, p, defaults.indexOfLastSelectedEcLevel)
		json.put(FREE_ROTATION, p, defaults.freeRotation)
		json.put(EXPAND_ESCAPE_SEQUENCES, p, defaults.expandEscapeSequences)
		json.put(ADD_QUIET_ZONE, p, defaults.addQuietZone)
		json.put(BRIGHTEN_SCREEN, p, defaults.brightenScreen)
		json.put(PREVIEW_SCALE, p, defaults.previewScale)
		json.put(AUTOMATED_ACTIONS, p.getString(AUTOMATED_ACTIONS, null) ?: "[]")

		return json.toString()
	}

	fun importProfile(context: Context, text: String): String? {
		val json = try {
			JSONObject(text)
		} catch (_: Exception) {
			return null
		}

		if (json.optString("type") != PROFILE_EXPORT_TYPE) {
			return null
		}

		var sanitized = json.optString("name").ifEmpty {
			"imported"
		}.replace(Regex("[^a-zA-Z0-9_-]+"), "_")
			.trim('_')
		if (sanitized.isEmpty()) {
			sanitized = "imported"
		}

		val name = if (profiles.contains(sanitized)) {
			val timestamp = SimpleDateFormat(
				"yyyyMMdd_HHmmss",
				Locale.US
			).format(Date())
			"${sanitized}_$timestamp"
		} else {
			sanitized
		}
		if (!addProfile(name)) {
			return null
		}

		val target = context.getSharedPreferences(
			name,
			Context.MODE_PRIVATE
		)
		try {
			target.edit {
				val formatsArray = json.optJSONArray(BARCODE_FORMATS)
				if (formatsArray != null) {
					val formats = mutableSetOf<String>()
					for (i in 0 until formatsArray.length()) {
						val format = formatsArray.getString(i)
							.migrateBarcodeFormatName()
						try {
							formats.add(BarcodeFormat.valueOf(format).name)
						} catch (_: IllegalArgumentException) {
							// Ignore unknown formats.
						}
					}
					if (formats.isNotEmpty()) {
						putStringSet(BARCODE_FORMATS, formats)
					}
				}
				putBoolean(SHOW_CROP_HANDLE, json)
				putBoolean(SHOW_CROSSHAIRS, json)
				putBoolean(ZOOM_BY_SWIPING, json)
				putBoolean(AUTO_ROTATE, json)
				putBoolean(TRY_HARDER, json)
				putBoolean(BULK_MODE, json)
				putString(BULK_MODE_DELAY, json)
				putBoolean(SHOW_TOAST_IN_BULK_MODE, json)
				putBoolean(VIBRATE, json)
				putBoolean(BEEP, json)
				putString(BEEP_TONE_NAME, json)
				putString(BEEP_STREAM_NAME, json)
				putBoolean(USE_HISTORY, json)
				putString(IGNORE_DUPLICATES_NAME, json)
				putString(IGNORE_CODES, json)
				putBoolean(COPY_IMMEDIATELY, json)
				putBoolean(OPEN_IMMEDIATELY, json)
				putBoolean(SHOW_META_DATA, json)
				putBoolean(SHOW_HEX_DUMP, json)
				putString(SHOW_CHECKSUM, json)
				putBoolean(SHOW_RECREATION, json)
				putBoolean(CLOSE_AUTOMATICALLY, json)
				putString(DEFAULT_SEARCH_URL, json)
				putString(OPEN_WITH_URL, json)
				putBoolean(SEND_SCAN_ACTIVE, json)
				putString(SEND_SCAN_URL, json)
				putString(SEND_SCAN_TYPE, json)
				putBoolean(SEND_SCAN_BLUETOOTH, json)
				putString(SEND_SCAN_BLUETOOTH_HOST, json)
				putString(CUSTOM_LOCALE, json)
				putInt(INDEX_OF_LAST_SELECTED_FORMAT, json)
				putInt(INDEX_OF_LAST_SELECTED_EC_LEVEL, json)
				putBoolean(FREE_ROTATION, json)
				putBoolean(EXPAND_ESCAPE_SEQUENCES, json)
				putBoolean(ADD_QUIET_ZONE, json)
				putBoolean(BRIGHTEN_SCREEN, json)
				putFloat(PREVIEW_SCALE, json)
				putString(AUTOMATED_ACTIONS, json)
			}
		} catch (_: Exception) {
			removeProfile(name)
			target.edit {
				clear()
			}
			return null
		}

		return name
	}

	fun update() {
		preferences.getStringSet(BARCODE_FORMATS, barcodeFormats)?.let {
			barcodeFormats = addFormatsOnUpdate(
				migrateCode39ToCode39Std(
					migrateBarcodeFormatNames(it)
				),
				BarcodeFormat.Code39Std,
				BarcodeFormat.Code39Ext,
				BarcodeFormat.Code32,
				BarcodeFormat.PZN,
				BarcodeFormat.DataBarOmni,
				BarcodeFormat.DataBarStk,
				BarcodeFormat.DataBarLtd,
				BarcodeFormat.DataBarExp,
				BarcodeFormat.DataBarExpStk,
				BarcodeFormat.RMQRCode,
				BarcodeFormat.DXFilmEdge
			)
		}

		showCropHandle = preferences.getBoolean(
			SHOW_CROP_HANDLE,
			showCropHandle
		)
		showCrosshairs = preferences.getBoolean(
			SHOW_CROSSHAIRS,
			showCrosshairs
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
		preferences.getString(BEEP_STREAM_NAME, beepStreamName)?.also {
			beepStreamName = it
		}
		useHistory = preferences.getBoolean(USE_HISTORY, useHistory)

		// Map old setting to new one if it wasn't on default value.
		val ignoreConsecutiveDuplicates = "ignore_consecutive_duplicates"
		if (!preferences.getBoolean(ignoreConsecutiveDuplicates, true)) {
			preferences.edit {
				remove(ignoreConsecutiveDuplicates)
			}
			ignoreDuplicatesName = "accept_duplicates"
		}

		preferences.getString(
			IGNORE_DUPLICATES_NAME,
			ignoreDuplicatesName
		)?.also {
			ignoreDuplicatesName = it
		}
		ignoreCodes = ignoreCodesFromJsonArray(
			preferences.getString(
				IGNORE_CODES,
				ignoreCodesToJsonArray(
					listOf(IgnoreCode(DEFAULT_IGNORE_CODE_PATTERN))
				)
			) ?: "[]"
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

	fun setIgnoreCodes(patterns: List<IgnoreCode>) {
		ignoreCodes = patterns.toMutableList()
		apply(IGNORE_CODES, ignoreCodesToJsonArray(patterns))
	}

	fun shouldIgnoreHistoryContent(content: String, format: String): Boolean =
		ignoreCodes.any { it.matches(content, format) }

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

	fun beepStream() = when (beepStreamName) {
		"stream_notification" -> android.media.AudioManager.STREAM_NOTIFICATION
		else -> android.media.AudioManager.STREAM_MUSIC
	}

	fun ignoreDuplicates() = when (ignoreDuplicatesName) {
		"ignore_consecutive_duplicates" -> IgnoreDuplicates.Consecutive
		"ignore_any_duplicates" -> IgnoreDuplicates.Any
		else -> IgnoreDuplicates.Never
	}

	private fun migrateCode39ToCode39Std(
		restored: Set<String>
	) = restored.toMutableSet().apply {
		// Migrate Code39 to the more specific Code39Std.
		if (contains(BarcodeFormat.Code39.name)) {
			remove(BarcodeFormat.Code39.name)
			if (!contains(BarcodeFormat.Code39Std.name)) {
				add(BarcodeFormat.Code39Std.name)
			}
		}
	}

	private fun migrateBarcodeFormatNames(formats: Set<String>): Set<String> {
		return formats.mapTo(mutableSetOf()) { format ->
			format.migrateBarcodeFormatName()
		}
	}

	private fun addFormatsOnUpdate(
		restored: Set<String>,
		vararg formats: BarcodeFormat
	) = restored.toMutableSet().apply {
		for (format in formats) {
			val name = "${format.name}_added"
			if (!preferences.getBoolean(name, false)) {
				preferences.edit {
					putBoolean(name, true)
				}
				add(format.name)
			}
		}
	}

	private fun apply(label: String, value: Boolean) {
		preferences.edit {
			putBoolean(label, value)
		}
	}

	private fun apply(label: String, value: String) {
		preferences.edit {
			putString(label, value)
		}
	}

	private fun apply(label: String, value: Int) {
		preferences.edit {
			putInt(label, value)
		}
	}

	private fun apply(label: String, value: Float) {
		preferences.edit {
			putFloat(label, value)
		}
	}

	@SuppressLint("ApplySharedPref")
	private fun commit(label: String, value: String) {
		preferences.edit(true) {
			putString(label, value)
		}
	}

	private fun apply(label: String, value: Set<String>) {
		preferences.edit {
			putStringSet(label, value)
		}
	}

	companion object {
		enum class IgnoreDuplicates {
			Consecutive, Any, Never
		}

		private const val PROFILE_EXPORT_TYPE = "binaryeye_profile"
		private const val PROFILES = "profiles"
		private const val PROFILE = "profile"
		private const val BARCODE_FORMATS = "formats"
		private const val SHOW_CROP_HANDLE = "show_crop_handle"
		private const val SHOW_CROSSHAIRS = "show_crosshairs"
		private const val ZOOM_BY_SWIPING = "zoom_by_swiping"
		private const val AUTO_ROTATE = "auto_rotate"
		private const val TRY_HARDER = "try_harder"
		private const val BULK_MODE = "bulk_mode"
		private const val BULK_MODE_DELAY = "bulk_mode_delay"
		private const val SHOW_TOAST_IN_BULK_MODE = "show_toast_in_bulk_mode"
		private const val VIBRATE = "vibrate"
		private const val BEEP = "beep"
		private const val BEEP_TONE_NAME = "beep_tone_name"
		private const val BEEP_STREAM_NAME = "beep_stream_name"
		private const val USE_HISTORY = "use_history"
		private const val IGNORE_DUPLICATES_NAME = "ignore_duplicates_name"
		private const val IGNORE_CODES = "ignore_codes"
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
		private const val DEFAULT_IGNORE_CODE_PATTERN = "^FIDO://.*"
	}
}

private fun JSONObject.put(key: String, p: SharedPreferences, default: Boolean) {
	put(key, p.getBoolean(key, default))
}

private fun JSONObject.put(key: String, p: SharedPreferences, default: String) {
	put(key, p.getString(key, null) ?: default)
}

private fun JSONObject.put(key: String, p: SharedPreferences, default: Int) {
	put(key, p.getInt(key, default))
}

private fun JSONObject.put(key: String, p: SharedPreferences, default: Float) {
	put(key, p.getFloat(key, default))
}

private fun SharedPreferences.Editor.putBoolean(key: String, json: JSONObject) {
	if (json.has(key)) {
		putBoolean(key, json.getBoolean(key))
	}
}

private fun SharedPreferences.Editor.putString(key: String, json: JSONObject) {
	if (json.has(key)) {
		putString(key, json.getString(key))
	}
}

private fun SharedPreferences.Editor.putInt(key: String, json: JSONObject) {
	if (json.has(key)) {
		putInt(key, json.getInt(key))
	}
}

private fun SharedPreferences.Editor.putFloat(key: String, json: JSONObject) {
	if (json.has(key)) {
		putFloat(key, json.getDouble(key).toFloat())
	}
}
