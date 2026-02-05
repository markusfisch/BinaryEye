package de.markusfisch.android.binaryeye.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v14.preference.MultiSelectListPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceGroup
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.activity.SplashActivity
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.hasBluetoothPermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.bluetooth.setBluetoothHosts
import de.markusfisch.android.binaryeye.media.beepConfirm
import de.markusfisch.android.binaryeye.preference.UrlPreference
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.view.systemBarRecyclerViewScrollListener
import de.markusfisch.android.binaryeye.widget.toast

class PreferencesFragment : PreferenceFragmentCompat() {
	private var lastProfile: String? = null

	private val changeListener = object : OnSharedPreferenceChangeListener {
		override fun onSharedPreferenceChanged(
			sharedPreferences: SharedPreferences?,
			key: String?
		) {
			key ?: return
			val preference = findPreference(key) ?: return
			if (preference.key != PROFILE) {
				prefs.update()
			}
			when (preference.key) {
				"custom_locale" -> {
					activity?.restartApp()
					return
				}

				"beep_tone_name" -> beepConfirm()

				"send_scan_bluetooth" -> if (
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
			.registerOnSharedPreferenceChangeListener(changeListener)
		setSummaries(preferenceScreen)

		wireProfiles()
		wireAutomatedActions()
		setBluetoothResources()
		wireClearNetworkPreferences()
	}

	private fun wireProfiles() {
		findPreference(PROFILE).apply {
			updateProfileSummary(this)
			onPreferenceClickListener = Preference.OnPreferenceClickListener {
				activity?.supportFragmentManager?.addFragment(
					ProfilesFragment()
				)
				true
			}
		}
	}

	private fun wireAutomatedActions() {
		findPreference(AUTOMATED_ACTIONS).apply {
			updateAutomatedActionsSummary(this)
			onPreferenceClickListener = Preference.OnPreferenceClickListener {
				activity?.supportFragmentManager?.addFragment(
					AutomatedActionsFragment()
				)
				true
			}
		}
	}

	private fun setBluetoothResources() {
		if (prefs.sendScanBluetooth &&
			activity?.hasBluetoothPermission() == true
		) {
			setBluetoothHosts(
				findPreference("send_scan_bluetooth_host") as ListPreference
			)
		}
	}

	private fun wireClearNetworkPreferences() {
		findPreference("clear_network_suggestions").apply {
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
				// From R+ we can query past network suggestions and
				// make them editable.
				setOnPreferenceClickListener {
					activity?.supportFragmentManager?.addFragment(
						NetworkSuggestionsFragment()
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
		AlertDialog.Builder(this)
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
		findPreference(AUTOMATED_ACTIONS)?.let {
			updateAutomatedActionsSummary(it)
		}
		findPreference(PROFILE)?.let {
			updateProfileSummary(it)
		}
		listView.setPaddingFromWindowInsets()
		listView.removeOnScrollListener(systemBarRecyclerViewScrollListener)
		listView.addOnScrollListener(systemBarRecyclerViewScrollListener)
	}

	override fun onPause() {
		super.onPause()
		preferenceScreen.sharedPreferences
			.unregisterOnSharedPreferenceChangeListener(changeListener)
	}

	override fun onDisplayPreferenceDialog(preference: Preference) {
		if (preference is UrlPreference) {
			val fm = fragmentManager
			UrlDialogFragment.newInstance(preference.key).apply {
				setTargetFragment(this@PreferencesFragment, 0)
				show(fm, null)
			}
		} else if (preference.key == "send_scan_bluetooth_host") {
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
		if (preference.key == AUTOMATED_ACTIONS) {
			updateAutomatedActionsSummary(preference)
			return
		}
		when (preference) {
			is UrlPreference -> {
				preference.summary = preference.getUrl()
			}

			is ListPreference -> {
				preference.setSummary(preference.entry)
			}

			is MultiSelectListPreference -> {
				preference.summary = preference.values.joinToString(", ") {
					it.replace(Regex("_"), " ")
				}
			}

			is PreferenceGroup -> {
				setSummaries(preference)
			}
		}
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

	private fun updateProfileSummary(preference: Preference) {
		preference.summary = prefs.profile ?: getString(R.string.profile_default)
		lastProfile = prefs.profile
	}

	companion object {
		private const val PROFILE = "profile"
		private const val AUTOMATED_ACTIONS = "automated_actions"
	}
}

private fun Activity.restartApp() {
	val intent = Intent(this, SplashActivity::class.java)
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		intent.addFlags(
			Intent.FLAG_ACTIVITY_NEW_TASK or
					Intent.FLAG_ACTIVITY_CLEAR_TASK
		)
	}
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
