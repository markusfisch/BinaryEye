package de.markusfisch.android.binaryeye.fragment

import android.os.Bundle
import android.support.v7.preference.PreferenceDialogFragmentCompat
import android.view.View
import android.widget.TextView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.preference.UrlPreference

class UrlDialogFragment : PreferenceDialogFragmentCompat() {
	private var urlView: TextView? = null

	override fun onBindDialogView(view: View?) {
		super.onBindDialogView(view)
		urlView = view?.findViewById(R.id.url)
		urlView?.text = urlPreference().getUrl()
	}

	override fun onDialogClosed(positiveResult: Boolean) {
		if (positiveResult) {
			urlPreference().setUrl(urlView?.text.toString())
		}
	}

	private fun urlPreference() = preference as UrlPreference

	companion object {
		fun newInstance(key: String): UrlDialogFragment {
			val args = Bundle()
			args.putString(ARG_KEY, key)
			val fragment = UrlDialogFragment()
			fragment.arguments = args
			return fragment
		}
	}
}
