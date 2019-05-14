package de.markusfisch.android.binaryeye.graphics

import android.graphics.Bitmap
import android.graphics.Matrix

val matrix = Matrix()
var buf: IntArray? = null

fun lumaToBitmap(
	yuv: ByteArray,
	width: Int,
	height: Int,
	orientation: Int,
	destWidth: Int,
	destHeight: Int
): Bitmap {
	// convert luma component only; ZXing will binarize the input image
	// anyway so there's no need to preserve color information
	val bitmap = lumaToBitmap(yuv, width, height)
	var dw = destWidth
	var dh = destHeight
	if (orientation == 90 || orientation == 270) {
		dw = destHeight
		dh = destWidth
	}
	matrix.setScale(
		dw.toFloat() / width.toFloat(),
		dh.toFloat() / height.toFloat()
	)
	matrix.postRotate(orientation.toFloat())
	return Bitmap.createBitmap(
		bitmap,
		0,
		0,
		width,
		height,
		matrix,
		true
	)
}

fun lumaToBitmap(yuv: ByteArray, width: Int, height: Int): Bitmap {
	val size = width * height
	// reuse buf to avoid allocation in hot path
	val b = buf ?: IntArray(size)
	buf = b
	for (i in 0 until size) {
		val l = yuv[i].toInt() and 255
		b[i] = 0xff000000.toInt() or (l shl 16) or (l shl 8) or l
	}
	return Bitmap.createBitmap(b, width, height, Bitmap.Config.ARGB_8888)
}
