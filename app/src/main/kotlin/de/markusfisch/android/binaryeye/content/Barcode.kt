package de.markusfisch.android.binaryeye.content

import android.graphics.Bitmap
import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.binaryeye.graphics.COLOR_BLACK
import de.markusfisch.android.binaryeye.graphics.COLOR_WHITE
import de.markusfisch.android.zxingcpp.ZxingCpp
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import de.markusfisch.android.zxingcpp.ZxingCpp.BitMatrix
import de.markusfisch.android.zxingcpp.ZxingCpp.toBitmap
import de.markusfisch.android.zxingcpp.ZxingCpp.toSvg
import de.markusfisch.android.zxingcpp.ZxingCpp.toText
import java.util.Arrays

enum class BarcodeColors {
	BLACK_ON_WHITE,
	WHITE_ON_BLACK,
	BLACK_ON_TRANSPARENT,
	WHITE_ON_TRANSPARENT;

	fun foregroundColor(): Int = when (this) {
		BLACK_ON_WHITE,
		BLACK_ON_TRANSPARENT -> COLOR_BLACK

		WHITE_ON_BLACK,
		WHITE_ON_TRANSPARENT -> COLOR_WHITE
	}

	fun backgroundColor(): Int = when (this) {
		BLACK_ON_WHITE -> COLOR_WHITE
		WHITE_ON_BLACK -> COLOR_BLACK
		BLACK_ON_TRANSPARENT,
		WHITE_ON_TRANSPARENT -> 0
	}
}

fun Scan.toBarcode(): Barcode<*> {
	val content = text.ifEmpty {
		raw
	}
	return if (symbol != null) {
		BitMatrixBarcode(
			symbol,
			content,
			format
		)
	} else {
		ContentBarcode(
			content,
			format,
			errorCorrectionLevel.toErrorCorrectionInt()
		)
	}
}

fun String?.toErrorCorrectionInt() = when (this) {
	"L" -> 0
	"M" -> 4
	"Q" -> 6
	"H" -> 8
	else -> -1
}

sealed class Barcode<T>(
	val content: T,
	val format: BarcodeFormat,
	val colors: BarcodeColors
) {
	private var _bitmap: Bitmap? = null
	fun bitmap(): Bitmap {
		val b = _bitmap ?: bitmap(512)
		_bitmap = b
		return b
	}

	fun bitmap(size: Int): Bitmap {
		return toBitmap(size)
	}

	protected abstract fun toBitmap(size: Int): Bitmap

	private var _svg: String? = null
	fun svg(): String {
		val s = _svg ?: toSvg()
		_svg = s
		return s
	}

	protected abstract fun toSvg(): String

	private var _text: String? = null
	fun text(): String {
		val t = _text ?: toText()
		_text = t
		return t
	}

	protected abstract fun toText(): String

	fun textOrHex(): String = when (content) {
		is String -> content
		is ByteArray -> content.toHexString()
		else -> throw IllegalArgumentException("Illegal arguments")
	}
}

class ContentBarcode<T>(
	content: T,
	format: BarcodeFormat,
	val ecLevel: Int = -1,
	val margin: Int = 1,
	colors: BarcodeColors = BarcodeColors.BLACK_ON_WHITE
) : Barcode<T>(content, format, colors) {
	override fun toBitmap(size: Int) = ZxingCpp.encodeAsBitmap(
		content, format, size, format.height(size), margin, ecLevel,
		setColor = colors.foregroundColor(),
		unsetColor = colors.backgroundColor()
	)

	override fun toSvg() = ZxingCpp.encodeAsSvg(
		content, format, margin, ecLevel
	)

	override fun toText() = ZxingCpp.encodeAsText(
		content, format, margin, ecLevel,
		inverted = colors == BarcodeColors.BLACK_ON_WHITE
	)
}

class BitMatrixBarcode<T>(
	val bitMatrix: BitMatrix,
	content: T,
	format: BarcodeFormat,
	colors: BarcodeColors = BarcodeColors.BLACK_ON_WHITE
) : Barcode<T>(content, format, colors) {
	override fun toBitmap(size: Int) = bitMatrix.inflate(size).toBitmap(
		setColor = colors.foregroundColor(),
		unsetColor = colors.backgroundColor()
	)

	override fun toSvg() = bitMatrix.inflate().toSvg()

	override fun toText() = bitMatrix.inflate().toText(
		inverted = colors == BarcodeColors.BLACK_ON_WHITE
	)
}

private fun BitMatrix.inflate(
	size: Int = 0,
	quietZone: Boolean = true
): BitMatrix {
	val extraModules = if (quietZone) 2 else 0
	val codeWidth = width + extraModules
	val codeHeight = height + extraModules
	var scale = size / codeWidth
	if (scale < 1) {
		if (size > 0 && !quietZone) {
			return this
		}
		scale = 1
	}
	val outWidth = codeWidth * scale
	val outHeight = codeHeight * scale
	val scaled = BitMatrix(
		outWidth,
		outHeight,
		ByteArray(outWidth * outHeight)
	)
	Arrays.fill(scaled.data, 1)
	val padding = extraModules / 2
	val left = padding * scale
	val top = padding * scale
	val right = outWidth - left
	val bottom = outHeight - top
	for (y in top until bottom) {
		var dst = y * outWidth + left
		val offset = (y - top) / scale * width
		for (x in left until right) {
			scaled.data[dst++] = data[
				offset + (x - left) / scale
			]
		}
	}
	return scaled
}

private fun BarcodeFormat.height(size: Int) = when (this) {
	// 1D barcodes don't need as much vertical space.
	BarcodeFormat.CODABAR,
	BarcodeFormat.CODE_39,
	BarcodeFormat.CODE_93,
	BarcodeFormat.CODE_128,
	BarcodeFormat.DATA_BAR,
	BarcodeFormat.DATA_BAR_EXPANDED,
	BarcodeFormat.DX_FILM_EDGE,
	BarcodeFormat.EAN_8,
	BarcodeFormat.EAN_13,
	BarcodeFormat.ITF,
	BarcodeFormat.UPC_A,
	BarcodeFormat.UPC_E -> size / 3

	else -> size
}