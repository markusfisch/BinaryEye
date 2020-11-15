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
		if (mr != lastMappedRect) {
			removeCallbacks(onScanRunnable)
			postDelayed(onScanRunnable, 300)
			lastMappedRect.set(mr)
		}
	}
}
