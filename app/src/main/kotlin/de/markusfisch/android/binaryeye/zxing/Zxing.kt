package de.markusfisch.android.binaryeye.zxing

import android.graphics.Bitmap
import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import java.util.*

class Zxing(possibleResultPoint: ResultPointCallback? = null) {
	private val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
	private val multiFormatReader = MultiFormatReader()

	init {
		val decodeFormats = EnumSet.noneOf(
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
		hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
		possibleResultPoint?.let {
			hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = it
		}
	}

	fun updateHints(tryHarder: Boolean) {
		if (tryHarder) {
			hints[DecodeHintType.TRY_HARDER] = java.lang.Boolean.TRUE
		} else {
			hints.remove(DecodeHintType.TRY_HARDER)
		}
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

	fun decodePositiveNegative(bitmap: Bitmap): Result? =
		decode(bitmap, false) ?: decode(bitmap, true)

	private fun decode(bitmap: Bitmap, invert: Boolean = false): Result? {
		val pixels = IntArray(bitmap.width * bitmap.height)
		return decode(pixels, bitmap, invert)
	}

	private fun decode(
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
			multiFormatReader.decode(bitmap, hints)
		} catch (e: ReaderException) {
			null
		} finally {
			multiFormatReader.reset()
		}
	}
}

private const val BLACK = 0xff000000.toInt()
private const val WHITE = 0xffffffff.toInt()

fun encodeAsBitmap(
	text: String,
	format: BarcodeFormat,
	width: Int,
	height: Int,
	hints: EnumMap<EncodeHintType, Any>? = null
): Bitmap? {
	val bitMatrix = encode(text, format, hints, width, height)
	val w = bitMatrix.width
	val h = bitMatrix.height
	val pixels = IntArray(w * h)
	var offset = 0
	for (y in 0 until h) {
		for (x in 0 until w) {
			pixels[offset + x] = if (bitMatrix.get(x, y)) {
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

fun encodeAsSvg(
	text: String,
	format: BarcodeFormat,
	hints: EnumMap<EncodeHintType, Any>? = null
): String {
	val bitMatrix = encode(text, format, hints)
	val sb = StringBuilder()
	val w = bitMatrix.width
	var h = bitMatrix.height
	val moduleHeight = if (h == 1) w / 2 else 1
	for (y in 0 until h) {
		for (x in 0 until w) {
			if (bitMatrix.get(x, y)) {
				sb.append(" M${x},${y}h1v${moduleHeight}h-1z")
			}
		}
	}
	h *= moduleHeight
	return """<svg width="$w" height="$h"
viewBox="0 0 $w $h"
xmlns="http://www.w3.org/2000/svg">
<path d="$sb"/>
</svg>
"""
}

fun encodeAsText(
	text: String,
	format: BarcodeFormat,
	hints: EnumMap<EncodeHintType, Any>? = null
): String {
	val bitMatrix = encode(text, format, hints)
	val w = bitMatrix.width
	val h = bitMatrix.height
	val sb = StringBuilder()
	for (y in 0 until h) {
		for (x in 0 until w) {
			sb.append(if (bitMatrix.get(x, y)) "█" else " ")
		}
		sb.append("\n")
	}
	return sb.toString()
}

private fun encode(
	text: String,
	format: BarcodeFormat,
	encodeHints: EnumMap<EncodeHintType, Any>? = null,
	width: Int = 0,
	height: Int = 0
): BitMatrix {
	val hints = encodeHints ?: EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
	if (!hints.contains(EncodeHintType.CHARACTER_SET)) {
		hints[EncodeHintType.CHARACTER_SET] = "utf-8"
	}
	return MultiFormatWriter().encode(
		text,
		format,
		width,
		height,
		hints
	)
}
