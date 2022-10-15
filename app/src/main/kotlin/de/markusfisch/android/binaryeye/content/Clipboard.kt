package de.markusfisch.android.binaryeye.content

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

fun Context.copyToClipboard(text: String, isSensitive: Boolean = false) {
	val service = getSystemService(Context.CLIPBOARD_SERVICE)
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
		@Suppress("DEPRECATION")
		(service as android.text.ClipboardManager).text = text
	} else {
		(service as android.content.ClipboardManager).setPrimaryClip(
			ClipData.newPlainText("plain text", text).apply {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					description.extras = PersistableBundle().apply {
						putBoolean(
							ClipDescription.EXTRA_IS_SENSITIVE,
							isSensitive
						)
					}
				}
			}
		)
	}
}
