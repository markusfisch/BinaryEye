package de.markusfisch.android.binaryeye.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val IMAGE_TARGET_SIZE = 1024
private const val MAX_IMAGE_FILE_SIZE = 64 * 1024 * 1024

fun Context.loadImageUri(uri: Uri): Bitmap? = try {
	val file = File.createTempFile("image-import-", null, cacheDir)
	try {
		if (copyImageUri(uri, file)) file.decodeSampledImage() else null
	} finally {
		file.delete()
	}
} catch (_: IOException) {
	null
} catch (_: SecurityException) {
	null
} catch (_: IllegalArgumentException) {
	null
} catch (_: OutOfMemoryError) {
	null
}

private fun Context.copyImageUri(uri: Uri, file: File): Boolean {
	val stream = contentResolver.openInputStream(uri) ?: return false
	return stream.use { input ->
		file.outputStream().use { output ->
			val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
			var remaining = MAX_IMAGE_FILE_SIZE
			while (remaining > 0) {
				val count = input.read(buffer, 0, min(buffer.size, remaining))
				if (count < 0) {
					return true
				}
				output.write(buffer, 0, count)
				remaining -= count
			}
			input.read() < 0
		}
	}
}

private fun File.decodeSampledImage(): Bitmap? {
	val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
	BitmapFactory.decodeFile(path, options)
	if (options.outWidth <= 0 || options.outHeight <= 0) {
		return null
	}
	options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight)
	options.inJustDecodeBounds = false
	return BitmapFactory.decodeFile(path, options)?.fixTransparency()
}

private fun calculateInSampleSize(
	width: Int,
	height: Int,
	reqWidth: Int = IMAGE_TARGET_SIZE,
	reqHeight: Int = IMAGE_TARGET_SIZE
): Int {
	var inSampleSize = 1
	if (height > reqHeight || width > reqWidth) {
		val halfHeight = height / 2
		val halfWidth = width / 2
		while (
			halfHeight / inSampleSize >= reqHeight ||
			halfWidth / inSampleSize >= reqWidth
		) {
			inSampleSize *= 2
		}
	}
	return inSampleSize
}

fun Bitmap.crop(
	rect: RectF,
	rotation: Float,
	pivotX: Float,
	pivotY: Float
) = try {
	val b = rotate(rotation, pivotX, pivotY)
	val w = b.width
	val h = b.height
	val x = max(0, (rect.left * w).roundToInt())
	val y = max(0, (rect.top * h).roundToInt())
	Bitmap.createBitmap(
		b,
		x,
		y,
		min(w, (rect.right * w).roundToInt()) - x,
		min(h, (rect.bottom * h).roundToInt()) - y
	)
} catch (e: OutOfMemoryError) {
	null
} catch (e: IllegalArgumentException) {
	null
}

private fun Bitmap.rotate(
	rotation: Float,
	pivotX: Float,
	pivotY: Float
): Bitmap = if (rotation % 360f != 0f) {
	Bitmap.createBitmap(
		this,
		0,
		0,
		width,
		height,
		Matrix().apply {
			setRotate(rotation, pivotX, pivotY)
		},
		true
	)
} else {
	this
}
