package de.markusfisch.android.binaryeye.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import de.markusfisch.android.binaryeye.R

class UrlPreference(
	context: Context,
	attrs: AttributeSet?
) : DialogPreference(context, attrs) {
	private var url: String? = null

	init {
		dialogLayoutResource = R.layout.dialog_url
	}

	fun getUrl() = url

	fun setUrl(url: String) {
		this.url = url
		persistString(url)
	}

	override fun onSetInitialValue(
		restorePersistedValue: Boolean,
		defaultValue: Any?
	) {
		setUrl(
			if (restorePersistedValue) {
				getPersistedString(url)
			} else {
				defaultValue as String
			}
		)
	}
}
