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
		val files = assets.list("yuv") ?: return
		for (file in files) {
			val m = pattern.matcher(file)
			if (!m.find() || m.groupCount() < 3) {
				continue
			}
			val frameWidth = m.group(1) ?: return
			val frameHeight = m.group(2) ?: return
			val frameOrientation = m.group(3) ?: return
			val frameData = assets.open("yuv/$file").readBytes()
			val preprocessor = Preprocessor(
				InstrumentationRegistry.getTargetContext(),
				frameWidth.toInt(),
				frameHeight.toInt()
			)
			val outWidth: Int
			val outHeight: Int
			val orientation = frameOrientation.toInt()
			if (orientation == 90 || orientation == 270) {
				preprocessor.resizeAndRotate(frameData)
				outWidth = preprocessor.outHeight
				outHeight = preprocessor.outWidth
			} else {
				preprocessor.resizeOnly(frameData)
				outWidth = preprocessor.outWidth
				outHeight = preprocessor.outHeight
			}
			val result = zxing.decode(frameData, outWidth, outHeight)
			preprocessor.destroy()

			checkNotNull(result) { "no barcode found in $file" }
		}
	}
}
