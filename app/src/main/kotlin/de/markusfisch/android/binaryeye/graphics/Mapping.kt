package de.markusfisch.android.binaryeye.graphics

import android.graphics.Matrix
import android.graphics.Rect
import com.google.zxing.ResultPoint

fun mapResultToView(
	frameWidth: Int,
	frameHeight: Int,
	frameOrientation: Int,
	viewRect: Rect,
	resultPoints: Array<ResultPoint?>,
	targetArray: FloatArray
): Int = getFrameToViewMatrix(
	frameWidth,
	frameHeight,
	frameOrientation,
	viewRect
).map(
	resultPoints,
	targetArray
)

fun getFrameToViewMatrix(
	frameWidth: Int,
	frameHeight: Int,
	frameOrientation: Int,
	viewRect: Rect
) = Matrix().apply {
	// Normalize to frame dimensions for rotation.
	postScale(
		1f / frameWidth,
		1f / frameHeight
	)
	// Rotate around center.
	postRotate(frameOrientation.toFloat(), 0.5f, 0.5f)
	// Scale up to view size.
	postScale(viewRect.width().toFloat(), viewRect.height().toFloat())
	// Apply view displacement.
	postTranslate(viewRect.left.toFloat(), viewRect.top.toFloat())
}

fun Matrix.map(
	resultPoints: Array<ResultPoint?>,
	targetArray: FloatArray
): Int {
	val max = targetArray.size
	var i = 0
	for (resultPoint in resultPoints) {
		// Because ZXing apparently returns null in this array sometimes.
		if (resultPoint == null) {
			continue
		}
		targetArray[i++] = resultPoint.x
		targetArray[i++] = resultPoint.y
		if (i >= max) {
			break
		}
	}
	mapPoints(targetArray, 0, targetArray, 0, i)
	return i
}
