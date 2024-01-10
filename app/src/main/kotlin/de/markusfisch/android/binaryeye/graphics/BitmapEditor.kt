package de.markusfisch.android.binaryeye.graphics

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun ContentResolver.loadImageUri(uri: Uri): Bitmap? = try {
	val options = BitmapFactory.Options()
	openInputStream(uri)?.use {
		options.inJustDecodeBounds = true
		BitmapFactory.decodeStream(it, null, options)
		options.inSampleSize = calculateInSampleSize(
			options.outWidth,
			options.outHeight
		)
		options.inJustDecodeBounds = false
	}
	openInputStream(uri)?.use {
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
