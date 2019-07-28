package de.markusfisch.android.binaryeye.fragment

import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.R

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

class PreferencesFragment : Fragment() {
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

		initHistorySwitch(view.findViewById(R.id.use_history) as SwitchCompat)
		initOpenWithUrl(view.findViewById(R.id.open_with_url) as EditText)

		return view
	}
}

fun initHistorySwitch(switchView: SwitchCompat) {
	switchView.setOnCheckedChangeListener { _, isChecked ->
		prefs.useHistory = isChecked
	}
	if (prefs.useHistory) {
		switchView.toggle()
	}
}

fun initOpenWithUrl(editText: EditText) {
	editText.setText(prefs.openWithUrl)
	editText.addTextChangedListener(object : TextWatcher {
		override fun afterTextChanged(s: Editable?) {
			prefs.openWithUrl = s.toString()
		}

		override fun beforeTextChanged(s: CharSequence?, start: Int,
			count: Int, after: Int
		) {
		}

		override fun onTextChanged(s: CharSequence?, start: Int, before: Int,
			count: Int
		) {
		}
	})
}
