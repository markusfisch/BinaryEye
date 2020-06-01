package de.markusfisch.android.binaryeye.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.support.v4.content.ContextCompat
import de.markusfisch.android.binaryeye.R

class Dots(context: Context) {
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val radius = 8f * context.resources.displayMetrics.density

	init {
		paint.color = ContextCompat.getColor(
			context,
			R.color.dot
		)
		paint.style = Paint.Style.FILL
	}

	fun draw(canvas: Canvas, points: List<Point>) {
		for (point in points) {
			canvas.drawCircle(
				point.x.toFloat(),
				point.y.toFloat(),
				radius,
				paint
			)
		}
	}
}
