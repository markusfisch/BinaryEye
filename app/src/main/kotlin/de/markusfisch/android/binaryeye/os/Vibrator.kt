package de.markusfisch.android.binaryeye.os

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

private const val DURATION_IN_MS = 250L

private val beforeO = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
private val errorPatternTimings = longArrayOf(
	50L, 100L, 50L, 100L, 50L
)
private val errorPatternAmplitudes = intArrayOf(
	255, 0, 255, 0, 255
)

// On SDK 31 and better, we should ask the `VIBRATOR_MANAGER_SERVICE`
// for the default vibrator. This was implemented here before:
/*fun Context.getVibrator(): Vibrator = if (
	Build.VERSION.SDK_INT < Build.VERSION_CODES.S
) {
	@Suppress("DEPRECATION")
	getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
} else {
	(getSystemService(
		Context.VIBRATOR_MANAGER_SERVICE
	) as VibratorManager).defaultVibrator
}*/
// But for reasons beyond my interests, using `VIBRATOR_MANAGER_SERVICE`
// leads to a very cryptic `java.lang.VerifyError: r0/b` on Android 4.
// This is probably a Proguard/R8 issue, but I don't want to waste more
// time just to get rid of the deprecation warning. So let's use the old
// `VIBRATOR_SERVICE` and wait for Google to fix their stuff.
fun Context.getVibrator(): Vibrator = getSystemService(
	Context.VIBRATOR_SERVICE
) as Vibrator

fun Vibrator.vibrate() {
	if (beforeO) {
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
	if (beforeO) {
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
