package de.markusfisch.android.binaryeye.fragment

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceGroup
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.activity.SplashActivity
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.preference.UrlPreference
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.view.systemBarRecyclerViewScrollListener

class PreferencesFragment : PreferenceFragmentCompat() {
	private val changeListener = object : OnSharedPreferenceChangeListener {
		override fun onSharedPreferenceChanged(
			sharedPreferences: SharedPreferences,
			key: String
		) {
			val preference = findPreference(key) ?: return
			prefs.update()
			if (preference.key == "custom_locale") {
				activity?.restartApp()
			} else {
				setSummary(preference)
			}
		}
	}

	override fun onCreatePreferences(state: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.preferences)
		activity?.setTitle(R.string.preferences)
	}

	override fun onResume() {
		super.onResume()
		listView.setPaddingFromWindowInsets()
		listView.removeOnScrollListener(systemBarRecyclerViewScrollListener)
		listView.addOnScrollListener(systemBarRecyclerViewScrollListener)
		preferenceScreen.sharedPreferences
			.registerOnSharedPreferenceChangeListener(changeListener)
		setSummaries(preferenceScreen)
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
		when (preference) {
			is UrlPreference -> {
				preference.setSummary(preference.getUrl())
			}
			is ListPreference -> {
				preference.setSummary(preference.entry)
			}
			is PreferenceGroup -> {
				setSummaries(preference)
			}
		}
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
