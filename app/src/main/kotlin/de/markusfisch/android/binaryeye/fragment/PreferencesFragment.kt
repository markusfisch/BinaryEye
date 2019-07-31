package de.markusfisch.android.binaryeye.fragment

import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.R

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

class PreferencesFragment : Fragment() {
	private lateinit var useHistorySwitch: SwitchCompat
	private lateinit var openWithUrlInput: EditText

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		activity.setTitle(R.string.preferences)

		val view = inflater.inflate(
			R.layout.fragment_preferences,
			container,
			false
		)

		useHistorySwitch = view.findViewById<SwitchCompat>(R.id.use_history)
		if (prefs.useHistory) {
			useHistorySwitch.toggle()
		}

		openWithUrlInput = view.findViewById<EditText>(R.id.open_with_url)
		openWithUrlInput.setText(prefs.openWithUrl)

		return view
	}

	override fun onPause() {
		super.onPause()
		prefs.useHistory = useHistorySwitch.isChecked()
		prefs.openWithUrl = openWithUrlInput.text.toString()
	}
}
