package de.markusfisch.android.binaryeye.graphics

import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap

fun Resources.getBitmapFromDrawable(
	resId: Int
): Bitmap = getBitmapFromDrawable(resId, null)

fun Resources.getBitmapFromDrawable(
	resId: Int,
	theme: Theme?
): Bitmap = getBitmapFromDrawable(
	ResourcesCompat.getDrawable(this, resId, theme) ?: throw Resources.NotFoundException(
		"Drawable resource ID #0x${resId.toString(16)}"
	)
)

private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
	if (drawable is BitmapDrawable) {
		return drawable.bitmap
	}
	val bitmap = createBitmap(
		drawable.intrinsicWidth,
		drawable.intrinsicHeight,
		Bitmap.Config.ARGB_8888
	)
	val canvas = Canvas(bitmap)
	drawable.setBounds(0, 0, canvas.width, canvas.height)
	drawable.draw(canvas)
	return bitmap
}
