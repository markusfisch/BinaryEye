package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import de.markusfisch.android.binaryeye.graphics.Candidates
import de.markusfisch.android.binaryeye.graphics.getDashedBorderPaint
import kotlin.math.roundToInt

class CropImageView(context: Context, attr: AttributeSet) :
	ConfinedScalingImageView(context, attr) {
	val windowInsets = Rect()

	var onScan: (() -> List<Point>?)? = null

	private val candidates = Candidates(context)
	private val boundsPaint = context.getDashedBorderPaint()
	private val lastMappedRect = RectF()
	private val padding: Int = (24f * context.resources.displayMetrics.density).roundToInt()
	private val onScanRunnable = Runnable {
		onScan?.invoke()?.let {
			candidatePoints = it
			invalidate()
		}
	}

	private var candidatePoints: List<Point>? = null

	init {
		scaleType = ScaleType.CENTER_CROP
	}

	override fun onLayout(
		changed: Boolean,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int
	) {
		super.onLayout(changed, left, top, right, bottom)
		setBoundsWithPadding(
			left + windowInsets.left,
			top + windowInsets.top,
			right - windowInsets.right,
			bottom - windowInsets.bottom
		)
	}

	fun getBoundsRect() = Rect(
		bounds.left.roundToInt(),
		bounds.top.roundToInt(),
		bounds.right.roundToInt(),
		bounds.bottom.roundToInt()
	)

	private fun setBoundsWithPadding(
		left: Int,
		top: Int,
		right: Int,
		bottom: Int
	) {
		val width = right - left
		val height = bottom - top
		val size = if (width < height)
			width - padding * 2
		else
			height - padding * 2
		val hpad = (width - size) / 2
		val vpad = (height - size) / 2
		setBounds(
			left.toFloat() + hpad,
			top.toFloat() + vpad,
			right.toFloat() - hpad,
			bottom.toFloat() - vpad
		)
		center(bounds)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		canvas.drawRect(bounds, boundsPaint)
		candidatePoints?.let {
			candidates.draw(canvas, it)
		}
		candidatePoints = null
		val mr = mappedRect ?: return
		if (mr != lastMappedRect) {
			removeCallbacks(onScanRunnable)
			postDelayed(onScanRunnable, 500)
			lastMappedRect.set(mr)
		}
	}
}
