package de.markusfisch.android.binaryeye

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.support.v8.renderscript.RenderScript
import de.markusfisch.android.binaryeye.rs.Preprocessor
import de.markusfisch.android.binaryeye.zxing.Zxing
import org.junit.Assert.fail;
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
			"[0-9]+-([0-9]+)deg.jpg"
		)
		val samples = assets.list("samples")
		checkNotNull(samples) { "no samples found" }
		for (sample in samples) {
			val m = pattern.matcher(sample)
			if (!m.find() || m.groupCount() < 1) {
				fail("invalid sample: $sample")
			}
			val bitmap = BitmapFactory.decodeStream(
				assets.open("samples/$sample")
			)
			val frameData = getNV21(bitmap)
			val frameWidth = bitmap.width
			val frameHeight = bitmap.height
			val frameOrientation = m.group(1) ?: return
			val rs = RenderScript.create(
				InstrumentationRegistry.getTargetContext(),
			)
			val preprocessor = Preprocessor(
				rs,
				frameWidth,
				frameHeight,
				null
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
			rs.destroy()

			checkNotNull(result) { "no barcode found in $sample" }
		}
	}
}

fun getNV21(bitmap: Bitmap): ByteArray {
	val width = bitmap.width
	val height = bitmap.height
	val argb = IntArray(width * height)
	bitmap.getPixels(argb, 0, width, 0, 0, width, height)
	return encodeYUV420SP(argb, width, height)
}

private fun encodeYUV420SP(argb: IntArray, width: Int, height: Int): ByteArray {
	val yuv = ByteArray(ceilIfUneven(width) * ceilIfUneven(height) * 3 / 2)
	var yIndex = 0
	var uvIndex = width * height
	var r: Int
	var g: Int
	var b: Int
	var Y: Int
	var u: Int
	var v: Int
	var index = 0
	for (y in 0 until height) {
		for (x in 0 until width) {
			val pixel = argb[index]
			r = pixel and 0xff0000 shr 16
			g = pixel and 0xff00 shr 8
			b = pixel and 0xff
			Y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
			u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
			v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
			yuv[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
			if (y % 2 == 0 && index % 2 == 0) {
				yuv[uvIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
				yuv[uvIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
			}
			++index
		}
	}
	return yuv
}

private fun ceilIfUneven(n: Int) = if (n and 1 == 1) n + 1 else n
