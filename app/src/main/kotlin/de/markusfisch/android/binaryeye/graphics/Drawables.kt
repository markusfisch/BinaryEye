package de.markusfisch.android.binaryeye.graphics

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

fun Resources.getBitmapFromDrawable(
	resId: Int
): Bitmap = getBitmapFromDrawable(getDrawable(resId, null))

private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
	if (drawable is BitmapDrawable) {
		return drawable.bitmap
	}
	val bitmap = Bitmap.createBitmap(
		drawable.intrinsicWidth,
		drawable.intrinsicHeight,
		Bitmap.Config.ARGB_8888
	)
	val canvas = Canvas(bitmap)
	drawable.setBounds(0, 0, canvas.width, canvas.height)
	drawable.draw(canvas)
	return bitmap
}
