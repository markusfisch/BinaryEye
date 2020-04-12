package de.markusfisch.android.binaryeye.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.view.setPadding
import de.markusfisch.android.binaryeye.view.setWindowInsetListener

class PreferencesFragment : Fragment() {
	private lateinit var openImmediatelySwitch: SwitchCompat
	private lateinit var vibrateSwitch: SwitchCompat
	private lateinit var useHistorySwitch: SwitchCompat
	private lateinit var showMetaDataSwitch: SwitchCompat
	private lateinit var showHexDumpSwitch: SwitchCompat
	private lateinit var tryHarderSwitch: SwitchCompat
	private lateinit var ignoreConsecutiveDuplicatesSwitch: SwitchCompat
	private lateinit var openWithUrlInput: EditText

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		activity?.setTitle(R.string.preferences)

		val view = inflater.inflate(
			R.layout.fragment_preferences,
			container,
			false
		)

		openImmediatelySwitch = view.findViewById(R.id.open_immediately)
		if (prefs.openImmediately) {
			openImmediatelySwitch.toggle()
		}

		vibrateSwitch = view.findViewById(R.id.vibrate)
		if (prefs.vibrate) {
			vibrateSwitch.toggle()
		}

		useHistorySwitch = view.findViewById(R.id.use_history)
		if (prefs.useHistory) {
			useHistorySwitch.toggle()
		}

		ignoreConsecutiveDuplicatesSwitch = view.findViewById(
			R.id.ignore_consecutive_duplicates
		)
		if (prefs.ignoreConsecutiveDuplicates) {
			ignoreConsecutiveDuplicatesSwitch.toggle()
		}

		showMetaDataSwitch = view.findViewById(R.id.show_meta_data)
		if (prefs.showMetaData) {
			showMetaDataSwitch.toggle()
		}

		showHexDumpSwitch = view.findViewById(R.id.show_hex_dump)
		if (prefs.showHexDump) {
			showHexDumpSwitch.toggle()
		}

		tryHarderSwitch = view.findViewById(R.id.try_harder)
		if (prefs.tryHarder) {
			tryHarderSwitch.toggle()
		}

		openWithUrlInput = view.findViewById(R.id.open_with_url)
		openWithUrlInput.setText(prefs.openWithUrl)

		setWindowInsetListener { insets ->
			(view.findViewById(R.id.scroll_view) as View).setPadding(insets)
		}

		return view
	}

	override fun onPause() {
		super.onPause()
		prefs.openImmediately = openImmediatelySwitch.isChecked
		prefs.vibrate = vibrateSwitch.isChecked
		prefs.useHistory = useHistorySwitch.isChecked
		prefs.ignoreConsecutiveDuplicates = ignoreConsecutiveDuplicatesSwitch.isChecked
		prefs.showMetaData = showMetaDataSwitch.isChecked
		prefs.showHexDump = showHexDumpSwitch.isChecked
		prefs.tryHarder = tryHarderSwitch.isChecked
		prefs.openWithUrl = openWithUrlInput.text.toString()
	}
}
