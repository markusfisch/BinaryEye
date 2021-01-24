package de.markusfisch.android.binaryeye.os

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import de.markusfisch.android.binaryeye.app.prefs

private const val MILLISECONDS = 100L
private val errorPatternTimings = longArrayOf(
	50L, 100L, 50L, 100L, 50L
)
private val errorPatternAmplitudes = intArrayOf(
	255, 0, 255, 0, 255
)

fun Vibrator.vibrate() {
	if (!prefs.vibrate) {
		return
	}
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
		@Suppress("DEPRECATION")
		vibrate(MILLISECONDS)
	} else {
		vibrate(
			VibrationEffect.createOneShot(
				MILLISECONDS,
				VibrationEffect.DEFAULT_AMPLITUDE
			)
		)
	}
}

fun Vibrator.error() {
	if (!prefs.vibrate) {
		return
	}
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
		@Suppress("DEPRECATION")
		vibrate(errorPatternTimings, -1)
	} else {
		vibrate(
			VibrationEffect.createWaveform(
				errorPatternTimings,
				errorPatternAmplitudes,
				-1
			)
		)
	}
}
