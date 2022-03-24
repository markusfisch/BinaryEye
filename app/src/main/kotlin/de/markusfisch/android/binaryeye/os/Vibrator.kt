package de.markusfisch.android.binaryeye.os

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import de.markusfisch.android.binaryeye.app.prefs

private const val DURATION_IN_MS = 100L

private val beforeO = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
private val errorPatternTimings = longArrayOf(
	50L, 100L, 50L, 100L, 50L
)
private val errorPatternAmplitudes = intArrayOf(
	255, 0, 255, 0, 255
)

fun Context.getVibrator(): Vibrator = if (
	Build.VERSION.SDK_INT < Build.VERSION_CODES.S
) {
	@Suppress("DEPRECATION")
	getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
} else {
	(getSystemService(
		Context.VIBRATOR_MANAGER_SERVICE
	) as VibratorManager).defaultVibrator
}

fun Vibrator.vibrate() {
	if (!prefs.vibrate) {
		return
	} else if (beforeO) {
		@Suppress("DEPRECATION")
		vibrate(DURATION_IN_MS)
	} else {
		vibrate(
			VibrationEffect.createOneShot(
				DURATION_IN_MS,
				VibrationEffect.DEFAULT_AMPLITUDE
			)
		)
	}
}

fun Vibrator.error() {
	if (!prefs.vibrate) {
		return
	} else if (beforeO) {
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
