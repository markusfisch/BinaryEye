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
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets

class PreferencesFragment : Fragment() {
	private lateinit var showCropHandleSwitch: SwitchCompat
	private lateinit var zoomBySwipingSwitch: SwitchCompat
	private lateinit var autoResizeSwitch: SwitchCompat
	private lateinit var tryHarderSwitch: SwitchCompat
	private lateinit var vibrateSwitch: SwitchCompat
	private lateinit var useHistorySwitch: SwitchCompat
	private lateinit var ignoreConsecutiveDuplicatesSwitch: SwitchCompat
	private lateinit var openImmediatelySwitch: SwitchCompat
	private lateinit var showMetaDataSwitch: SwitchCompat
	private lateinit var showHexDumpSwitch: SwitchCompat
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

		showCropHandleSwitch = view.findViewById(R.id.show_crop_handle)
		if (prefs.showCropHandle) {
			showCropHandleSwitch.toggle()
		}

		zoomBySwipingSwitch = view.findViewById(R.id.zoom_by_swiping)
		if (prefs.zoomBySwiping) {
			zoomBySwipingSwitch.toggle()
		}

		autoResizeSwitch = view.findViewById(R.id.auto_rotate)
		if (prefs.autoRotate) {
			autoResizeSwitch.toggle()
		}

		tryHarderSwitch = view.findViewById(R.id.try_harder)
		if (prefs.tryHarder) {
			tryHarderSwitch.toggle()
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

		openImmediatelySwitch = view.findViewById(R.id.open_immediately)
		if (prefs.openImmediately) {
			openImmediatelySwitch.toggle()
		}

		showMetaDataSwitch = view.findViewById(R.id.show_meta_data)
		if (prefs.showMetaData) {
			showMetaDataSwitch.toggle()
		}

		showHexDumpSwitch = view.findViewById(R.id.show_hex_dump)
		if (prefs.showHexDump) {
			showHexDumpSwitch.toggle()
		}

		openWithUrlInput = view.findViewById(R.id.open_with_url)
		openWithUrlInput.setText(prefs.openWithUrl)

		(view.findViewById(R.id.scroll_view) as View).setPaddingFromWindowInsets()

		return view
	}

	override fun onStop() {
		super.onStop()
		prefs.showCropHandle = showCropHandleSwitch.isChecked
		prefs.zoomBySwiping = zoomBySwipingSwitch.isChecked
		prefs.autoRotate = autoResizeSwitch.isChecked
		prefs.tryHarder = tryHarderSwitch.isChecked
		prefs.vibrate = vibrateSwitch.isChecked
		prefs.useHistory = useHistorySwitch.isChecked
		prefs.ignoreConsecutiveDuplicates = ignoreConsecutiveDuplicatesSwitch.isChecked
		prefs.openImmediately = openImmediatelySwitch.isChecked
		prefs.showMetaData = showMetaDataSwitch.isChecked
		prefs.showHexDump = showHexDumpSwitch.isChecked
		prefs.openWithUrl = openWithUrlInput.text.toString()
	}
}
