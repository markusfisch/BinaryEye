package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.windowInsets
import kotlin.math.roundToInt

class CropImageView(context: Context, attr: AttributeSet) :
	ConfinedScalingImageView(context, attr) {
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val padding: Int = (context.resources.displayMetrics.density * 24f).roundToInt()

	init {
		paint.color = ContextCompat.getColor(context, R.color.crop_bound)
		paint.style = Paint.Style.STROKE
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
		canvas.drawRect(bounds, paint)
	}
}
