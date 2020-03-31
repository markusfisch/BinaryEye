package de.markusfisch.android.binaryeye.graphics

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import java.io.IOException
import kotlin.math.roundToInt

fun loadImageUri(cr: ContentResolver, uri: Uri): Bitmap? = try {
	val options = BitmapFactory.Options()
	cr.openInputStream(uri)?.use {
		options.inJustDecodeBounds = true
		BitmapFactory.decodeStream(it, null, options)
		options.inSampleSize = calculateInSampleSize(
			options.outWidth,
			options.outHeight
		)
		options.inJustDecodeBounds = false
	}
	cr.openInputStream(uri)?.use {
		BitmapFactory.decodeStream(it, null, options)
	}
} catch (e: IOException) {
	null
}

private fun calculateInSampleSize(
	width: Int,
	height: Int,
	reqWidth: Int = 1024,
	reqHeight: Int = 1024
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
