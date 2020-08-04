package de.markusfisch.android.binaryeye.fragment

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceGroup
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.app.systemBarRecyclerViewScrollListener
import de.markusfisch.android.binaryeye.preference.UrlPreference
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets

class PreferencesFragment : PreferenceFragmentCompat() {
	private val changeListener = object : OnSharedPreferenceChangeListener {
		override fun onSharedPreferenceChanged(
			sharedPreferences: SharedPreferences,
			key: String
		) {
			val preference = findPreference(key) ?: return
			prefs.update()
			setSummary(preference)
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
			is PreferenceGroup -> {
				setSummaries(preference)
			}
		}
	}
}
