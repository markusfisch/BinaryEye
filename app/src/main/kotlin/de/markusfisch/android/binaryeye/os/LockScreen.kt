package de.markusfisch.android.binaryeye.os

import android.app.Activity
import android.os.Build
import android.view.WindowManager

@Suppress("DEPRECATION")
fun Activity.showWhenLocked() {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
		setShowWhenLocked(true)
	} else {
		window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
	}
}
