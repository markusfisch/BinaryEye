package de.markusfisch.android.binaryeye.zxing

import android.graphics.Bitmap
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.*

class Zxing(possibleResultPoint: ResultPointCallback? = null) {
	private val multiFormatReader = MultiFormatReader()

	init {
		val decodeFormats = EnumSet.noneOf<BarcodeFormat>(
			BarcodeFormat::class.java
		)
		decodeFormats.addAll(
			EnumSet.copyOf(
				listOf(
					BarcodeFormat.AZTEC,
					BarcodeFormat.CODABAR,
					BarcodeFormat.CODE_39,
					BarcodeFormat.CODE_93,
					BarcodeFormat.CODE_128,
					BarcodeFormat.DATA_MATRIX,
					BarcodeFormat.EAN_8,
					BarcodeFormat.EAN_13,
					BarcodeFormat.ITF,
					BarcodeFormat.MAXICODE,
					BarcodeFormat.PDF_417,
					BarcodeFormat.QR_CODE,
					BarcodeFormat.RSS_14,
					BarcodeFormat.RSS_EXPANDED,
					BarcodeFormat.UPC_A,
					BarcodeFormat.UPC_E,
					BarcodeFormat.UPC_EAN_EXTENSION
				)
			)
		)

		val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
		hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
		possibleResultPoint?.let {
			hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = it
		}

		multiFormatReader.setHints(hints)
	}

	fun decode(
		yuvData: ByteArray,
		width: Int,
		height: Int,
		invert: Boolean = false
	): Result? {
		val source = PlanarYUVLuminanceSource(
			yuvData,
			width,
			height,
			0,
			0,
			width,
			height,
			false
		)
		return decodeLuminanceSource(source, invert)
	}

	fun decodePositiveNegative(bitmap: Bitmap): Result? {
		val result = decode(bitmap, false)
		return if (result != null) {
			result
		} else {
			decode(bitmap, true)
		}
	}

	fun decode(bitmap: Bitmap, invert: Boolean = false): Result? {
		val pixels = IntArray(bitmap.width * bitmap.height)
		return decode(pixels, bitmap, invert)
	}

	fun decode(
		pixels: IntArray,
		bitmap: Bitmap,
		invert: Boolean = false
	): Result? {
		val width = bitmap.width
		val height = bitmap.height
		if (bitmap.config != Bitmap.Config.ARGB_8888) {
			bitmap.copy(Bitmap.Config.ARGB_8888, true)
		} else {
			bitmap
		}.getPixels(pixels, 0, width, 0, 0, width, height)
		return decodeLuminanceSource(
			RGBLuminanceSource(width, height, pixels),
			invert
		)
	}

	private fun decodeLuminanceSource(
		source: LuminanceSource,
		invert: Boolean
	): Result? {
		return decodeLuminanceSource(
			if (invert) {
				source.invert()
			} else {
				source
			}
		)
	}

	private fun decodeLuminanceSource(source: LuminanceSource): Result? {
		val bitmap = BinaryBitmap(HybridBinarizer(source))
		return try {
			multiFormatReader.decodeWithState(bitmap)
		} catch (e: ReaderException) {
			null
		} finally {
			multiFormatReader.reset()
		}
	}

	companion object {
		private const val BLACK = 0xff000000.toInt()
		private const val WHITE = 0xffffffff.toInt()

		fun encodeAsBitmap(
			text: String,
			format: BarcodeFormat,
			width: Int,
			height: Int
		): Bitmap? {
			val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
			hints[EncodeHintType.CHARACTER_SET] = "utf-8"
			val result = MultiFormatWriter().encode(
				text,
				format,
				width,
				height,
				hints
			)
			val w = result.width
			val h = result.height
			val pixels = IntArray(w * h)
			var offset = 0
			for (y in 0 until h) {
				for (x in 0 until w) {
					pixels[offset + x] = if (result.get(x, y)) {
						BLACK
					} else {
						WHITE
					}
				}
				offset += w
			}
			val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
			bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
			return bitmap
		}
	}
}
