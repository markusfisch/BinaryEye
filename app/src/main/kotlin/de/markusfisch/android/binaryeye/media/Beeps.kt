package de.markusfisch.android.binaryeye.media

import android.media.AudioManager
import android.media.ToneGenerator
import de.markusfisch.android.binaryeye.app.prefs

private var confirmToneGenerator: ToneGenerator? = null
private var confirmToneStream = AudioManager.STREAM_MUSIC
private var errorToneGenerator: ToneGenerator? = null

fun beepConfirm() {
	val stream = prefs.beepStream()
	val tg = if (confirmToneGenerator == null || confirmToneStream != stream) {
		confirmToneGenerator?.release()
		ToneGenerator(stream, ToneGenerator.MAX_VOLUME).also {
			confirmToneGenerator = it
			confirmToneStream = stream
		}
	} else {
		confirmToneGenerator!!
	}
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
	confirmToneStream = AudioManager.STREAM_MUSIC
	errorToneGenerator?.release()
	errorToneGenerator = null
}
