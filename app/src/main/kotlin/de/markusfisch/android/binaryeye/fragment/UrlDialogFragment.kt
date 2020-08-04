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
		urlView?.text = (preference as UrlPreference).getUrl()
	}

	override fun onDialogClosed(positiveResult: Boolean) {
		if (positiveResult) {
			(preference as UrlPreference).setUrl(
				urlView?.text.toString()
			)
		}
	}

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
