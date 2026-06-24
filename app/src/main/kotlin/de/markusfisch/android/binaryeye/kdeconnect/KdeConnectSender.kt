package de.markusfisch.android.binaryeye.kdeconnect

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import de.markusfisch.android.binaryeye.content.startIntentOrToast
import de.markusfisch.android.binaryeye.database.Scan

private const val KDE_CONNECT_PACKAGE = "org.kde.kdeconnect_tp"
private const val KDE_CONNECT_KEYSTROKES_ACTIVITY =
	"org.kde.kdeconnect.plugins.mousepad.SendKeystrokesToHostActivity"
private const val KEYSTROKES_MIME_TYPE = "text/x-keystrokes"

fun Context.sendToKdeConnect(scan: Scan): Boolean {
	if (scan.text.isEmpty()) {
		return false
	}
	return startIntentOrToast(createKdeConnectIntent(scan.text))
}

fun createKdeConnectIntent(text: String): Intent {
	return Intent(Intent.ACTION_SEND).apply {
		component = ComponentName(
			KDE_CONNECT_PACKAGE,
			KDE_CONNECT_KEYSTROKES_ACTIVITY
		)
		type = KEYSTROKES_MIME_TYPE
		putExtra(Intent.EXTRA_TEXT, text)
	}
}
