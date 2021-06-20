package de.markusfisch.android.binaryeye.graphics

import android.graphics.Point
import android.graphics.Rect
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import kotlin.math.roundToInt

fun mapResult(
	frameWidth: Int,
	frameHeight: Int,
	frameOrientation: Int,
	viewRect: Rect,
	result: Result
): List<Point> = frameToView(
	frameWidth,
	frameHeight,
	frameOrientation,
	viewRect
).map(
	result.resultPoints
)

fun frameToView(
	frameWidth: Int,
	frameHeight: Int,
	frameOrientation: Int,
	viewRect: Rect
) = Mapping(
	frameWidth,
	frameHeight,
	frameOrientation,
	viewRect.width().toFloat(),
	viewRect.height().toFloat(),
	viewRect.left,
	viewRect.top
)

fun isPortrait(orientation: Int) = orientation == 90 || orientation == 270

data class Mapping(
	val frameWidth: Int,
	val frameHeight: Int,
	val frameOrientation: Int,
	val viewWidth: Float,
	val viewHeight: Float,
	val offsetX: Int,
	val offsetY: Int
) {
	val ratioX: Float
	val ratioY: Float

	init {
		val targetSize = if (isPortrait(frameOrientation)) {
			Point(frameHeight, frameWidth)
		} else {
			Point(frameWidth, frameHeight)
		}
		ratioX = viewWidth / targetSize.x.toFloat()
		ratioY = viewHeight / targetSize.y.toFloat()
	}

	fun map(resultPoint: ResultPoint): Point {
		val point = Point(
			resultPoint.x.roundToInt(),
			resultPoint.y.roundToInt()
		)
		rotate(point)
		point.set(
			(point.x * ratioX).roundToInt() + offsetX,
			(point.y * ratioY).roundToInt() + offsetY
		)
		return point
	}

	fun map(points: Array<ResultPoint?>): List<Point> =
		// Because ZXing apparently returns null in this array sometimes.
		points.filterNotNull().map { map(it) }

	private fun rotate(point: Point) = when (frameOrientation) {
		90 -> point.set(frameHeight - point.y, point.x)
		180 -> point.set(frameWidth - point.x, frameHeight - point.y)
		270 -> point.set(point.y, frameWidth - point.x)
		else -> {
		}
	}
}
