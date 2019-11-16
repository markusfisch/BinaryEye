package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.windowInsets
import kotlin.math.roundToInt

class CropImageView(context: Context, attr: AttributeSet) :
	ConfinedScalingImageView(context, attr) {
	var onScan: (() -> Rect)? = null

	private val boundsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val candidatePaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val lastMappedRect = RectF()
	private val dp = context.resources.displayMetrics.density
	private val padding: Int = (dp * 24f).roundToInt()
	private val onScanRunnable = Runnable {
		onScan?.invoke()?.also {
			candidateRect = it
			invalidate()
		}
	}

	private var candidateRect: Rect? = null

	init {
		boundsPaint.color = ContextCompat.getColor(context, R.color.crop_bound)
		boundsPaint.style = Paint.Style.STROKE
		boundsPaint.strokeWidth = dp * 2f
		boundsPaint.pathEffect = DashPathEffect(floatArrayOf(dp * 10f, dp * 10f), 0f)
		candidatePaint.color = ContextCompat.getColor(context, R.color.candidate)
		candidatePaint.style = Paint.Style.FILL_AND_STROKE
		candidatePaint.strokeWidth = dp * 3f
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

	fun getBoundsRect(): RectF {
		return bounds
	}

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
		val rc = candidateRect
		if (rc != null && rc.width() > 0) {
			canvas.drawRect(rc, candidatePaint)
			candidateRect = null
		}
		val mr = mappedRect ?: return
		if (mr != lastMappedRect) {
			removeCallbacks(onScanRunnable)
			postDelayed(onScanRunnable, 500)
			lastMappedRect.set(mr)
		}
	}
}
