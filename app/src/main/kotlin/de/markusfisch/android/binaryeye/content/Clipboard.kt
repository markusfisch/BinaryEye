package de.markusfisch.android.binaryeye.content

import android.content.ClipData
import android.content.Context
import android.os.Build

fun Context.copyToClipboard(text: String) {
	val service = getSystemService(Context.CLIPBOARD_SERVICE)
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
		@Suppress("DEPRECATION")
		(service as android.text.ClipboardManager).text = text
	} else {
		(service as android.content.ClipboardManager).setPrimaryClip(
			ClipData.newPlainText("plain text", text)
		)
	}
}