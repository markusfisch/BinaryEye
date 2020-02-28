package de.markusfisch.android.binaryeye.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import de.markusfisch.android.binaryeye.R
import kotlin.math.roundToInt

class Candidates(context: Context) {
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val radius = 8f * context.resources.displayMetrics.density

	init {
		paint.color = ContextCompat.getColor(
			context,
			R.color.candidate
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

fun mapResult(
	width: Int,
	height: Int,
	viewRect: Rect,
	result: Result
): List<Point> = frameToView(width, height, viewRect).map(result.resultPoints)

fun frameToView(width: Int, height: Int, viewRect: Rect) = Mapping(
	viewRect.width().toFloat() / width.toFloat(),
	viewRect.height().toFloat() / height.toFloat(),
	viewRect.left,
	viewRect.top
)

data class Mapping(
	val ratioX: Float,
	val ratioY: Float,
	val offsetX: Int,
	val offsetY: Int
) {
	fun map(points: Array<ResultPoint>): List<Point> {
		return points.map {
			Point(
				(it.x * ratioX).roundToInt() + offsetX,
				(it.y * ratioY).roundToInt() + offsetY
			)
		}
	}
}
