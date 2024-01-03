package de.markusfisch.android.binaryeye.media

import android.media.AudioManager
import android.media.ToneGenerator
import de.markusfisch.android.binaryeye.app.prefs

private var confirmToneGenerator: ToneGenerator? = null
private var errorToneGenerator: ToneGenerator? = null

fun beepConfirm() {
	val tg = confirmToneGenerator ?: ToneGenerator(
		AudioManager.STREAM_NOTIFICATION,
		ToneGenerator.MAX_VOLUME
	)
	confirmToneGenerator = tg
	tg.startTone(prefs.beepTone())
}

fun beepError() {
	val tg = errorToneGenerator ?: ToneGenerator(
		AudioManager.STREAM_ALARM,
		ToneGenerator.MAX_VOLUME
	)
	errorToneGenerator = tg
	tg.startTone(ToneGenerator.TONE_SUP_ERROR, 1000)
}

fun releaseToneGenerators() {
	confirmToneGenerator?.release()
	confirmToneGenerator = null
	errorToneGenerator?.release()
	errorToneGenerator = null
}
