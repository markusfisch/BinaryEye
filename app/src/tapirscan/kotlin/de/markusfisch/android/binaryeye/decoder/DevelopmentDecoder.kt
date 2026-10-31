package de.markusfisch.android.binaryeye.decoder

import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import de.markusfisch.android.zxingcpp.ZxingCpp
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import de.markusfisch.android.zxingcpp.ZxingCpp.ReaderOptions
import de.markusfisch.android.zxingcpp.ZxingCpp.Result
import org.tapirscan.android.Format
import org.tapirscan.android.ScanOptions
import org.tapirscan.android.Scanner
import java.nio.ByteBuffer
import kotlin.math.atan2
import kotlin.math.roundToInt

/** Development-only fallback after ZXing. Never replaces encoding or existing reads. */
class DevelopmentDecoder : AutoCloseable {
	private var scanner: Scanner? = null
	private var disabled = false

	fun read(buffer: ByteBuffer, stride: Int, crop: Rect, rotation: Int, options: ReaderOptions): Result? {
		if (disabled || !options.tryRotate || options.textMode != ZxingCpp.TextMode.PLAIN ||
			options.eanAddOnSymbol != ZxingCpp.EanAddOnSymbol.IGNORE) return null
		val pairs = Format.ALL.mapNotNull { format ->
			toZxingFormat(format)?.let { format to it }
		}.filter { (_, target) ->
			options.formats.isEmpty() || BarcodeFormat.All in options.formats ||
				BarcodeFormat.AllReadable in options.formats || target in options.formats ||
				(target == BarcodeFormat.TelepenAlpha && BarcodeFormat.Telepen in options.formats)
		}
		if (pairs.isEmpty()) return null
		return try {
			val pixels = rotatedCrop(buffer, stride, crop, rotation)
			val swap = rotation % 180 != 0
			val width = if (swap) crop.height() else crop.width()
			val height = if (swap) crop.width() else crop.height()
			val active = scanner ?: Scanner().also { scanner = it }
			val result = active.scan(pixels, width, height, 1, 0,
				ScanOptions.defaults().withFormats(pairs.map { it.first }.toSet()))
			val barcode = result.barcodes().firstOrNull() ?: return null
			// Structured append and GS1 presentation need a richer result adapter.
			if (barcode.structuredAppend().isPresent || barcode.gs1().orElse(false)) return null
			val payload = plainTextBytes(barcode.format(), barcode.text(),
				barcode.payloadBytes().orElse(null)) ?: return null
			val points = barcode.polygon().map { Point(it.x().roundToInt(), it.y().roundToInt()) }
			val angle = Math.toDegrees(atan2((points[1].y - points[0].y).toDouble(),
				(points[1].x - points[0].x).toDouble()))
			Result(
				format = pairs.first { it.first == barcode.format() }.second,
				contentType = ZxingCpp.ContentType.TEXT,
				text = barcode.text(),
				position = ZxingCpp.Position(points[0], points[1], points[3], points[2], angle),
				orientation = angle.roundToInt(),
				rawBytes = payload,
				ecLevel = "", symbologyIdentifier = "", sequenceSize = -1,
				sequenceIndex = -1, sequenceId = "", readerInit = barcode.readerInitialization().orElse(false),
				dataMask = -1, lineCount = 0, version = "", symbol = null, gtin = null, error = null
			)
		} catch (error: LinkageError) {
			disabled = true
			Log.w("Tapirscan", "Development native library unavailable", error)
			null
		} catch (error: RuntimeException) {
			disabled = true
			Log.w("Tapirscan", "Development fallback disabled after failure", error)
			null
		}
	}

	override fun close() {
		scanner?.close()
		scanner = null
	}
}

/** Match ZXing's cropped, rotated coordinates without changing the camera buffer position. */
internal fun rotatedCrop(buffer: ByteBuffer, stride: Int, crop: Rect, rotation: Int): ByteArray {
	require(rotation in listOf(0, 90, 180, 270))
	require(crop.left >= 0 && crop.top >= 0 && crop.width() > 0 && crop.height() > 0)
	val width = crop.width()
	val height = crop.height()
	val out = ByteArray(Math.multiplyExact(width, height))
	val start = buffer.position()
	for (y in 0 until height) for (x in 0 until width) {
		val at = when (rotation) {
			90 -> x * height + height - 1 - y
			180 -> (height - 1 - y) * width + width - 1 - x
			270 -> (width - 1 - x) * height + y
			else -> y * width + x
		}
		out[at] = buffer.get(start + (crop.top + y) * stride + crop.left + x)
	}
	return out
}

/** Only rescue payloads whose byte and plain-text interpretations agree exactly. */
internal fun isPlainTextPayload(text: String, payload: ByteArray): Boolean =
	text.toByteArray(Charsets.UTF_8).contentEquals(payload) &&
		text.none { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }

internal fun toZxingFormat(format: Format): BarcodeFormat? {
	val name = when (format) {
		Format.CODE39 -> "Code39Std"
		Format.TELEPEN -> "TelepenAlpha"
		Format.DATA_BAR_EXPANDED -> "DataBarExp"
		Format.DATA_BAR_LIMITED -> "DataBarLtd"
		else -> format.toString()
	}
	return BarcodeFormat.entries.firstOrNull { it.name == name }
}

/** Linear readers report text without bytes. Reconstruct only unambiguous printable ASCII. */
internal fun plainTextBytes(format: Format, text: String, payload: ByteArray?): ByteArray? {
	if (payload != null) return payload.takeIf { isPlainTextPayload(text, it) }
	if (format !in Format.LINEAR || text.any { it.code !in 32..126 }) return null
	return text.toByteArray(Charsets.US_ASCII)
}
