package de.markusfisch.android.binaryeye.app

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

fun Vibrator.vibrate() {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
		@Suppress("DEPRECATION")
		this.vibrate(100)
	} else {
		this.vibrate(VibrationEffect.createOneShot(100, 10))
	}
}
