package de.markusfisch.android.binaryeye.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.database.Cursor
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.activity.AutomatedActionsActivity
import de.markusfisch.android.binaryeye.activity.IgnoreCodesActivity
import de.markusfisch.android.binaryeye.activity.NetworkSuggestionsActivity
import de.markusfisch.android.binaryeye.activity.ProfilesActivity
import de.markusfisch.android.binaryeye.activity.SplashActivity
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.app.hasBluetoothPermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.bluetooth.setBluetoothHosts
import de.markusfisch.android.binaryeye.camera.availableDefaultCameras
import de.markusfisch.android.binaryeye.camera.defaultCameraEntries
import de.markusfisch.android.binaryeye.media.beepConfirm
import de.markusfisch.android.binaryeye.preference.Preferences
import de.markusfisch.android.binaryeye.preference.UrlPreference
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.view.systemBarRecyclerViewScrollListener
import de.markusfisch.android.binaryeye.widget.toast

class PreferencesFragment : PreferenceFragmentCompat() {
	private var lastProfile: String? = null
	private val pickBeepTone = registerForActivityResult(
		ActivityResultContracts.OpenDocument()
	) { uri ->
		if (uri != null) {
			setCustomBeepTone(uri)
		}
	}

	private val changeListener = object : OnSharedPreferenceChangeListener {
		override fun onSharedPreferenceChanged(
			sharedPreferences: SharedPreferences?,
			key: String?
		) {
			key ?: return
			val preference = findPreference<Preference>(key) ?: return
			if (preference.key != PROFILE) {
				prefs.update()
			}
			when (preference.key) {
				CUSTOM_LOCALE,
				DYNAMIC_COLORS -> {
					activity?.restartApp()
					return
				}

				BEEP_TONE_NAME,
				BEEP_STREAM_NAME -> activity?.beepConfirm()

				SEND_SCAN_BLUETOOTH -> if (
					prefs.sendScanBluetooth &&
					activity?.hasBluetoothPermission() == false
				) {
					prefs.sendScanBluetooth = false
				}
			}
			setSummary(preference)
		}
	}

	override fun onCreatePreferences(state: Bundle?, rootKey: String?) {
		loadPreferences(rootKey)
	}

	private fun loadPreferences(rootKey: String? = null) {
		preferenceScreen?.sharedPreferences
			?.unregisterOnSharedPreferenceChangeListener(changeListener)

		preferenceManager.apply {
			sharedPreferencesName = prefs.profile ?: "${context.packageName}_preferences"
			sharedPreferencesMode = Context.MODE_PRIVATE
		}

		// Refresh the preferences.
		preferenceScreen = null
		setPreferencesFromResource(R.xml.preferences, rootKey)
		preferenceScreen.sharedPreferences
			?.registerOnSharedPreferenceChangeListener(changeListener)
		setSummaries(preferenceScreen)
		setIcons()

		wireBeepTonePicker()
		setDefaultCameras()
		wireProfiles()
		wireAutomatedActions()
		wireIgnoreCodes()
		setBluetoothResources()
		wireClearNetworkPreferences()
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
			findPreference<PreferenceGroup>("appearance_category")?.isVisible = false
		}
	}

	private fun wireProfiles() {
		findPreference<Preference>(PROFILE)?.apply {
			updateProfileSummary(this)
			onPreferenceClickListener = Preference.OnPreferenceClickListener {
				startActivity(Intent(activity, ProfilesActivity::class.java))
				true
			}
		}
	}

	private fun wireBeepTonePicker() {
		findPreference<ListPreference>(BEEP_TONE_NAME)?.apply {
			onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
				val toneName = newValue as? String ?: return@OnPreferenceChangeListener false
				if (toneName == Preferences.CUSTOM_BEEP_TONE_NAME) {
					pickBeepTone.launch(arrayOf("audio/*"))
					false
				} else {
					if (prefs.usesCustomBeepTone()) {
						clearCustomBeepTone()
					}
					prefs.beepToneName = toneName
					value = toneName
					setSummary(this)
					activity?.beepConfirm()
					false
				}
			}
		}
	}

	private fun setDefaultCameras() {
		findPreference<ListPreference>(DEFAULT_CAMERA)?.apply {
			val values = availableDefaultCameras(requireContext())
			entries = defaultCameraEntries(requireContext(), values)
			entryValues = values.toTypedArray()
			if (!values.contains(value)) {
				value = values.firstOrNull()
			}
			isVisible = values.size > 1
			setSummary(this)
		}
	}

	private fun wireAutomatedActions() {
		findPreference<Preference>(AUTOMATED_ACTIONS)?.apply {
			updateAutomatedActionsSummary(this)
			onPreferenceClickListener = Preference.OnPreferenceClickListener {
				startActivity(
					Intent(activity, AutomatedActionsActivity::class.java)
				)
				true
			}
		}
	}

	private fun wireIgnoreCodes() {
		findPreference<Preference>(IGNORE_CODES)?.apply {
			updateIgnoreCodesSummary(this)
			onPreferenceClickListener = Preference.OnPreferenceClickListener {
				startActivity(
					Intent(activity, IgnoreCodesActivity::class.java)
				)
				true
			}
		}
	}

	private fun setBluetoothResources() {
		if (prefs.sendScanBluetooth &&
			activity?.hasBluetoothPermission() == true
		) {
			findPreference<ListPreference>(SEND_SCAN_BLUETOOTH_HOST)?.let {
				setBluetoothHosts(it)
			}
		}
	}

	private fun wireClearNetworkPreferences() {
		findPreference<Preference>(CLEAR_NETWORK_SUGGESTIONS)?.apply {
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
				// From R+ we can query past network suggestions and
				// make them editable.
				setOnPreferenceClickListener {
					startActivity(
						Intent(activity, NetworkSuggestionsActivity::class.java)
					)
					true
				}
			} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
				// On Q, we can only clear *all* suggestions.
				// Note that previous versions of this app allowed
				// adding network suggestions on Q as well, so we
				// need to keep this option.
				setOnPreferenceClickListener {
					context.askToClearNetworkSuggestions()
					true
				}
			} else {
				// There are no network suggestions below Q.
				isVisible = false
			}
		}
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private fun Context.askToClearNetworkSuggestions() {
		MaterialAlertDialogBuilder(this)
			.setMessage(R.string.really_remove_all_networks)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				clearNetworkSuggestions()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.show()
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private fun Context.clearNetworkSuggestions() {
		toast(
			if (removeAllNetworkSuggestions() ==
				WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
			) {
				R.string.clear_network_suggestions_success
			} else {
				R.string.clear_network_suggestions_nothing_to_remove
			}
		)
	}

	override fun onResume() {
		super.onResume()
		if (lastProfile != prefs.profile) {
			loadPreferences()
		}
		activity?.setTitle(R.string.preferences)
		findPreference<Preference>(AUTOMATED_ACTIONS)?.let {
			updateAutomatedActionsSummary(it)
		}
		findPreference<Preference>(IGNORE_CODES)?.let {
			updateIgnoreCodesSummary(it)
		}
		findPreference<Preference>(PROFILE)?.let {
			updateProfileSummary(it)
		}
		listView.setPaddingFromWindowInsets()
		listView.removeOnScrollListener(systemBarRecyclerViewScrollListener)
		listView.addOnScrollListener(systemBarRecyclerViewScrollListener)
	}

	override fun onPause() {
		super.onPause()
		preferenceScreen.sharedPreferences
			?.unregisterOnSharedPreferenceChangeListener(changeListener)
	}

	override fun onDisplayPreferenceDialog(preference: Preference) {
		if (preference is UrlPreference) {
			val fm = parentFragmentManager
			UrlDialogFragment.newInstance(preference.key).apply {
				setTargetFragment(this@PreferencesFragment, 0)
				show(fm, null)
			}
		} else if (preference.key == SEND_SCAN_BLUETOOTH_HOST) {
			val ac = activity ?: return
			if (ac.hasBluetoothPermission()) {
				setBluetoothHosts(preference as ListPreference)
			}
			super.onDisplayPreferenceDialog(preference)
		} else {
			super.onDisplayPreferenceDialog(preference)
		}
	}

	private fun setSummaries(screen: PreferenceGroup) {
		var i = screen.preferenceCount
		while (i-- > 0) {
			setSummary(screen.getPreference(i))
		}
	}

	private fun setSummary(preference: Preference) {
		when (preference.key) {
			AUTOMATED_ACTIONS -> return updateAutomatedActionsSummary(preference)
			IGNORE_CODES -> return updateIgnoreCodesSummary(preference)
			BEEP_TONE_NAME -> {
				setBeepToneSummary(preference)
				return
			}
		}
		when (preference) {
			is EditTextPreference -> preference.summary = preference.text
			is UrlPreference -> preference.summary = preference.getUrl()
			is ListPreference -> preference.setSummary(preference.entry)
			is MultiSelectListPreference -> preference.summary =
				getSortedSummary(preference)

			is PreferenceGroup -> setSummaries(preference)
		}
	}

	private fun setBeepToneSummary(preference: Preference) {
		preference.summary = if (prefs.usesCustomBeepTone()) {
			getBeepToneLabel(prefs.beepToneUri.toUri())
		} else {
			findPreference<ListPreference>(BEEP_TONE_NAME)?.let {
				val index = it.findIndexOfValue(prefs.beepToneName)
				if (index >= 0) {
					it.entries[index]
				} else {
					""
				}
			} ?: ""
		}
	}

	private fun getSortedSummary(preference: MultiSelectListPreference): String {
		val labelsByValue = mutableMapOf<String, String>()
		val entries = preference.entries
		val entryValues = preference.entryValues
		val count = minOf(entries.size, entryValues.size)

		for (i in 0 until count) {
			labelsByValue[entryValues[i].toString()] = entries[i].toString()
		}

		return preference.values
			.map { value ->
				labelsByValue[value] ?: value.prettifyFormatName()
			}
			.sortedBy { it.lowercase() }
			.joinToString(", ")
	}

	private fun setIcons() {
		setIcon(PROFILE, R.drawable.ic_action_profile)
		setIcon("formats", R.drawable.ic_action_scan)
		setIcon("show_crop_handle", R.drawable.ic_action_crop)
		setIcon("show_crosshairs", R.drawable.ic_action_search)
		setIcon("zoom_by_swiping", R.drawable.ic_action_search)
		setIcon("auto_rotate", R.drawable.ic_action_down)
		setIcon("try_harder", R.drawable.ic_action_search)
		setIcon("bulk_mode", R.drawable.ic_action_bulk_mode)
		setIcon(DEFAULT_CAMERA, R.drawable.ic_action_switch_camera)
		setIcon("bulk_mode_delay", R.drawable.ic_action_delay)
		setIcon("show_toast_in_bulk_mode", R.drawable.ic_action_toast)
		setIcon("vibrate", R.drawable.ic_action_vibrate)
		setIcon("beep", R.drawable.ic_action_beep)
		setIcon(BEEP_TONE_NAME, R.drawable.ic_action_notification_sound)
		setIcon(BEEP_STREAM_NAME, R.drawable.ic_action_notification_sound)
		setIcon("use_history", R.drawable.ic_action_history)
		setIcon("ignore_duplicates_name", R.drawable.ic_action_remove)
		setIcon(IGNORE_CODES, R.drawable.ic_action_remove)
		setIcon("copy_immediately", R.drawable.ic_action_copy)
		setIcon("send_scan_active", R.drawable.ic_action_forward)
		setIcon("send_scan_url", R.drawable.ic_action_link)
		setIcon("send_scan_type", R.drawable.ic_action_link)
		setIcon("send_scan_device_id", R.drawable.ic_label)
		setIcon(SEND_SCAN_BLUETOOTH, R.drawable.ic_action_bluetooth)
		setIcon(SEND_SCAN_BLUETOOTH_HOST, R.drawable.ic_action_bluetooth)
		setIcon(SEND_SCAN_KDE_CONNECT, R.drawable.ic_action_forward)
		setIcon("open_immediately", R.drawable.ic_action_open)
		setIcon("strip_tracking_params", R.drawable.ic_action_remove)
		setIcon("show_meta_data", R.drawable.ic_action_info)
		setIcon("show_hex_dump", R.drawable.ic_action_info)
		setIcon("show_checksum", R.drawable.ic_action_info)
		setIcon("show_recreation", R.drawable.ic_action_scan)
		setIcon("close_automatically", R.drawable.ic_action_back)
		setIcon("default_search_url", R.drawable.ic_action_search)
		setIcon("open_with_url", R.drawable.ic_action_link)
		setIcon(AUTOMATED_ACTIONS, R.drawable.ic_action_create)
		setIcon(CLEAR_NETWORK_SUGGESTIONS, R.drawable.ic_action_wifi)
		setIcon("brighten_screen", R.drawable.ic_action_bright)
		setIcon(CUSTOM_LOCALE, R.drawable.ic_action_preferences)
		setIcon(DYNAMIC_COLORS, R.drawable.ic_action_preferences)
	}

	private fun setIcon(key: String, iconResId: Int) {
		findPreference<Preference>(key)?.setIcon(iconResId)
	}

	private fun updateAutomatedActionsSummary(preference: Preference) {
		val count = prefs.automatedActions.size
		preference.summary = if (count == 0) {
			getString(R.string.automated_actions_none)
		} else {
			resources.getQuantityString(
				R.plurals.automated_actions_count,
				count,
				count
			)
		}
	}

	private fun updateIgnoreCodesSummary(preference: Preference) {
		val count = prefs.ignoreCodes.size
		preference.summary = if (count == 0) {
			getString(R.string.ignore_codes_none)
		} else {
			resources.getQuantityString(
				R.plurals.ignore_codes_count,
				count,
				count
			)
		}
	}

	private fun updateProfileSummary(preference: Preference) {
		preference.summary = prefs.profileLabel(requireContext(), prefs.profile)
		lastProfile = prefs.profile
	}

	private fun setCustomBeepTone(uri: Uri) {
		val context = requireContext()
		val resolver = context.contentResolver
		clearCustomBeepTone()
		try {
			resolver.takePersistableUriPermission(
				uri,
				Intent.FLAG_GRANT_READ_URI_PERMISSION
			)
		} catch (e: SecurityException) {
			val message = e.message
			if (!message.isNullOrEmpty()) {
				context.toast(message)
			}
			return
		}
		prefs.beepToneUri = uri.toString()
		prefs.beepToneName = Preferences.CUSTOM_BEEP_TONE_NAME
		findPreference<ListPreference>(BEEP_TONE_NAME)?.apply {
			value = Preferences.CUSTOM_BEEP_TONE_NAME
			setSummary(this)
		}
		activity?.beepConfirm()
	}

	private fun clearCustomBeepTone(releaseSelection: Boolean = true) {
		if (releaseSelection) {
			releaseBeepTonePermission(prefs.beepToneUri)
		}
		prefs.beepToneUri = ""
	}

	private fun releaseBeepTonePermission(uriString: String) {
		if (uriString.isEmpty()) {
			return
		}
		try {
			requireContext().contentResolver.releasePersistableUriPermission(
				uriString.toUri(),
				Intent.FLAG_GRANT_READ_URI_PERMISSION
			)
		} catch (_: SecurityException) {
		}
	}

	private fun getBeepToneLabel(uri: Uri): String {
		var cursor: Cursor? = null
		return try {
			cursor = requireContext().contentResolver.query(
				uri,
				arrayOf(OpenableColumns.DISPLAY_NAME),
				null,
				null,
				null
			)
			if (cursor?.moveToFirst() == true) {
				cursor.getString(0)
			} else {
				uri.lastPathSegment ?: uri.toString()
			}
		} catch (_: Exception) {
			uri.lastPathSegment ?: uri.toString()
		} finally {
			cursor?.close()
		}
	}

	companion object {
		private const val PROFILE = "profile"
		private const val AUTOMATED_ACTIONS = "automated_actions"
		private const val BEEP_TONE_NAME = "beep_tone_name"
		private const val BEEP_STREAM_NAME = "beep_stream_name"
		private const val CLEAR_NETWORK_SUGGESTIONS = "clear_network_suggestions"
		private const val CUSTOM_LOCALE = "custom_locale"
		private const val DYNAMIC_COLORS = "dynamic_colors"
		private const val DEFAULT_CAMERA = "default_camera"
		private const val IGNORE_CODES = "ignore_codes"
		private const val SEND_SCAN_BLUETOOTH = "send_scan_bluetooth"
		private const val SEND_SCAN_BLUETOOTH_HOST = "send_scan_bluetooth_host"
		private const val SEND_SCAN_KDE_CONNECT = "send_scan_kde_connect"
	}
}

private fun Activity.restartApp() {
	val intent = Intent(this, SplashActivity::class.java)
	intent.addFlags(
		Intent.FLAG_ACTIVITY_NEW_TASK or
				Intent.FLAG_ACTIVITY_CLEAR_TASK
	)
	startActivity(intent)
	finish()
	// Restart to begin with an unmodified Locale to follow system settings.
	Runtime.getRuntime().exit(0)
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun Context.removeAllNetworkSuggestions(): Int =
	(applicationContext.getSystemService(
		Context.WIFI_SERVICE
	) as WifiManager).removeNetworkSuggestions(listOf())
