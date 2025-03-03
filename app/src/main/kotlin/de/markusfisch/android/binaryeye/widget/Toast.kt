package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.widget.Toast

private var lastToast: Toast? = null

fun <T> Context.toast(message: T) {
	lastToast?.cancel()
	lastToast = when (message) {
		is String -> Toast.makeText(
			applicationContext,
			message.ellipsize(),
			Toast.LENGTH_LONG
		)

		is Int -> Toast.makeText(
			applicationContext,
			message,
			Toast.LENGTH_LONG
		)

		else -> return
	}
	lastToast?.show()
}

private fun String.ellipsize(max: Int = 128) = if (length < max) {
	this
} else {
	"${take(max)}…"
}
