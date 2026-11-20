package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import kotlin.math.roundToInt

class CropImageView(context: Context, attr: AttributeSet) :
	ConfinedScalingImageView(context, attr) {
	var onScan: (() -> Unit)? = null

	private val lastMappedRect = RectF()
	private var lastRotation = Float.NaN
	private var lastPivotX = Float.NaN
	private var lastPivotY = Float.NaN
	private val onScanRunnable = Runnable { onScan?.invoke() }

	fun getBoundsRect() = Rect(
		bounds.left.roundToInt(),
		bounds.top.roundToInt(),
		bounds.right.roundToInt(),
		bounds.bottom.roundToInt()
	)

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val mr = mappedRect ?: return
		val rotation = imageRotation
		val x = pivotX
		val y = pivotY
		if (mr != lastMappedRect || rotation != lastRotation ||
			x != lastPivotX || y != lastPivotY
		) {
			removeCallbacks(onScanRunnable)
			postDelayed(onScanRunnable, 300)
			lastMappedRect.set(mr)
			lastRotation = rotation
			lastPivotX = x
			lastPivotY = y
		}
	}
}
