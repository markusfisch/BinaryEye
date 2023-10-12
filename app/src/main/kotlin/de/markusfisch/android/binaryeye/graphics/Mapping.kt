package de.markusfisch.android.binaryeye.graphics

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import de.markusfisch.android.zxingcpp.ZxingCpp.Position
import kotlin.math.roundToInt

data class FrameMetrics(
	var width: Int = 0,
	var height: Int = 0,
	var orientation: Int = 0
)

fun Rect.setFrameRoi(
	frameMetrics: FrameMetrics,
	viewRect: Rect,
	viewRoi: Rect
) {
	Matrix().apply {
		// Map ROI from view coordinates to frame coordinates.
		setTranslate(-viewRect.left.toFloat(), -viewRect.top.toFloat())
		postScale(1f / viewRect.width(), 1f / viewRect.height())
		postRotate(-frameMetrics.orientation.toFloat(), .5f, .5f)
		postScale(frameMetrics.width.toFloat(), frameMetrics.height.toFloat())
		val frameRoiF = RectF()
		val viewRoiF = RectF(
			viewRoi.left.toFloat(),
			viewRoi.top.toFloat(),
			viewRoi.right.toFloat(),
			viewRoi.bottom.toFloat()
		)
		mapRect(frameRoiF, viewRoiF)
		set(
			frameRoiF.left.roundToInt(),
			frameRoiF.top.roundToInt(),
			frameRoiF.right.roundToInt(),
			frameRoiF.bottom.roundToInt()
		)
	}
}

fun Matrix.setFrameToView(
	frameMetrics: FrameMetrics,
	viewRect: Rect,
	viewRoi: Rect? = null
) {
	// Configure this matrix to map points in frame coordinates to
	// view coordinates.
	val uprightWidth: Int
	val uprightHeight: Int
	when (frameMetrics.orientation) {
		90, 270 -> {
			uprightWidth = frameMetrics.height
			uprightHeight = frameMetrics.width
		}

		else -> {
			uprightWidth = frameMetrics.width
			uprightHeight = frameMetrics.height
		}
	}
	setScale(
		viewRect.width().toFloat() / uprightWidth,
		viewRect.height().toFloat() / uprightHeight
	)
	viewRoi?.let {
		postTranslate(viewRoi.left.toFloat(), viewRoi.top.toFloat())
	}
}

fun Matrix.mapPosition(
	position: Position,
	coords: FloatArray
): Int {
	var i = 0
	setOf(
		position.topLeft,
		position.topRight,
		position.bottomRight,
		position.bottomLeft
	).forEach {
		coords[i++] = it.x.toFloat()
		coords[i++] = it.y.toFloat()
	}
	mapPoints(coords)
	return i
}
