package de.markusfisch.android.binaryeye.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import androidx.core.net.toUri
import de.markusfisch.android.binaryeye.app.prefs

private var confirmToneGenerator: ToneGenerator? = null
private var confirmToneStream = AudioManager.STREAM_MUSIC
private var confirmMediaPlayer: MediaPlayer? = null
private var errorToneGenerator: ToneGenerator? = null

fun Context.beepConfirm() {
	val stream = prefs.beepStream()
	if (prefs.usesCustomBeepTone() &&
		playCustomConfirmTone(prefs.beepToneUri, stream)
	) {
		return
	}
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

private fun Context.playCustomConfirmTone(
	uriString: String,
	stream: Int
): Boolean {
	return try {
		confirmToneGenerator?.release()
		confirmToneGenerator = null
		confirmToneStream = stream
		confirmMediaPlayer?.release()
		confirmMediaPlayer = MediaPlayer().also { mp ->
			mp.apply {
				setAudioAttributes(
					AudioAttributes.Builder()
						.setLegacyStreamType(stream)
						.build()
				)
				setDataSource(this@playCustomConfirmTone, uriString.toUri())
				setOnCompletionListener { completed ->
					if (confirmMediaPlayer === completed) {
						confirmMediaPlayer = null
					}
					completed.release()
				}
				setOnErrorListener { failed, _, _ ->
					if (confirmMediaPlayer === failed) {
						confirmMediaPlayer = null
					}
					failed.release()
					true
				}
				prepare()
				start()
			}
		}
		true
	} catch (_: Exception) {
		confirmMediaPlayer?.release()
		confirmMediaPlayer = null
		false
	}
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
	confirmMediaPlayer?.release()
	confirmMediaPlayer = null
	errorToneGenerator?.release()
	errorToneGenerator = null
}
