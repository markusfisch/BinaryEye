package de.markusfisch.android.binaryeye.zxing

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

import android.graphics.Bitmap

import java.util.Arrays
import java.util.EnumMap
import java.util.EnumSet

class Zxing {
	private val multiFormatReader: MultiFormatReader = MultiFormatReader()

	init {
		val decodeFormats = EnumSet.noneOf<BarcodeFormat>(
				BarcodeFormat::class.java)
		decodeFormats.addAll(EnumSet.copyOf(Arrays.asList(
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
		)))

		val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
		hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats)

		multiFormatReader.setHints(hints)
	}

	fun decodeBitmap(bitmap: Bitmap): Result? {
		val width = bitmap.width
		val height = bitmap.height
		val data = IntArray(width * height)
		bitmap.getPixels(data, 0, width, 0, 0, width, height)
		return decodeLuminanceSource(RGBLuminanceSource(width, height, data))
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
}
