package de.markusfisch.android.binaryeye.graphics

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.roundToInt

fun downsizeIfBigger(bitmap: Bitmap, maxSize: Int): Bitmap = if (
	max(bitmap.width, bitmap.height) > maxSize
) {
	val srcWidth = bitmap.width
	val srcHeight = bitmap.height
	var newWidth = maxSize
	var newHeight = maxSize
	if (srcWidth > srcHeight) {
		newHeight = (newWidth.toFloat() / srcWidth.toFloat() *
				srcHeight.toFloat()).roundToInt()
	} else {
		newWidth = (newHeight.toFloat() / srcHeight.toFloat() *
				srcWidth.toFloat()).roundToInt()
	}
	Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
} else {
	bitmap
}

fun crop(bitmap: Bitmap, rect: RectF, rotation: Float) = try {
	val erected = erect(bitmap, rotation)
	val w = erected.width
	val h = erected.height
	Bitmap.createBitmap(
		erected,
		(rect.left * w).roundToInt(),
		(rect.top * h).roundToInt(),
		(rect.width() * w).roundToInt(),
		(rect.height() * h).roundToInt()
	)
} catch (e: OutOfMemoryError) {
	null
} catch (e: IllegalArgumentException) {
	null
}

private fun erect(bitmap: Bitmap, rotation: Float): Bitmap = if (
	rotation % 360f != 0f
) {
	val matrix = Matrix()
	matrix.setRotate(rotation)
	Bitmap.createBitmap(
		bitmap,
		0,
		0,
		bitmap.width,
		bitmap.height,
		matrix,
		true
	)
} else {
	bitmap
}
