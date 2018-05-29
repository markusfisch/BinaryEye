package de.markusfisch.android.binaryeye

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4

import de.markusfisch.android.binaryeye.rs.Preprocessor
import de.markusfisch.android.binaryeye.zxing.Zxing

import org.junit.Test
import org.junit.runner.RunWith

import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class PreprocessorTest {
	@Test
	fun processSamples() {
		val zxing = Zxing()
		val assets = InstrumentationRegistry.getInstrumentation()
			.context.assets
		val pattern = Pattern.compile(
			"[0-9]+-([0-9]+)x([0-9]+)-([0-9]+)deg.yuv"
		)

		for (file in assets.list("yuv")) {
			val m = pattern.matcher(file)
			if (!m.find() || m.groupCount() < 3) {
				continue
			}

			val frameWidth = m.group(1).toInt()
			val frameHeight = m.group(2).toInt()
			val frameOrientation = m.group(3).toInt()
			val frameData = assets.open("yuv/$file").readBytes()
			val preprocessor = Preprocessor(
				InstrumentationRegistry.getTargetContext(),
				frameWidth,
				frameHeight,
				frameOrientation
			)
			preprocessor.process(frameData)
			val result = zxing.decode(
				frameData,
				preprocessor.outWidth,
				preprocessor.outHeight
			)
			preprocessor.destroy()

			checkNotNull(result) { "no barcode found in $file" }
		}
	}
}
