package de.markusfisch.android.binaryeye.rs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.RenderScript

fun fixTransparency(rs: RenderScript, bitmap: Bitmap?): Bitmap? {
	bitmap ?: return null
	val result = analyze(rs, bitmap)
	return if (result.hasTransparentPixels) {
		val copy = Bitmap.createBitmap(
			bitmap.width,
			bitmap.height,
			bitmap.config
		)
		Canvas(copy).apply {
			drawColor(result.backgroundColor)
			drawBitmap(bitmap, 0f, 0f, null)
		}
		copy
	} else {
		bitmap
	}
}

private fun analyze(rs: RenderScript, bitmap: Bitmap): Result {
	val analyzeScript = ScriptC_analyze(rs)
	val bitmapAllocation = Allocation.createFromBitmap(rs, bitmap)
	val result = analyzeScript.reduce_analyze(bitmapAllocation).get()
	bitmapAllocation.destroy()
	analyzeScript.destroy()
	return Result(
		result.x > 0,
		if (result.y > 0) 0xff000000.toInt() else 0xffffffff.toInt()
	)
}

private data class Result(
	val hasTransparentPixels: Boolean,
	val backgroundColor: Int
)
