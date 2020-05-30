package de.markusfisch.android.binaryeye.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.graphics.Candidates
import de.markusfisch.android.binaryeye.graphics.getBitmapFromDrawable
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

class DetectorView : View {
	val roi = Rect()

	var updateRoi: (() -> Unit)? = null

	private val candidates = Candidates(context)
	private val invalidateRunnable: Runnable = Runnable {
		marks = null
		invalidate()
	}
	private val roiPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val handleBitmap = resources.getBitmapFromDrawable(
		R.drawable.ic_crop_handle
	)
	private val handleXRadius = handleBitmap.width / 2
	private val handleYRadius = handleBitmap.height / 2
	private val distToFull: Float

	private var marks: List<Point>? = null
	private var center = PointF()
	private var handlePos = PointF()
	private var handleGrabbed = false
	private var shadeColor = 0

	init {
		val dp = context.resources.displayMetrics.density
		distToFull = 24f * dp
		roiPaint.apply {
			style = Paint.Style.STROKE
			color = 0xffffffff.toInt()
			strokeWidth = 2f * dp
			pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
		}
	}

	constructor(context: Context, attrs: AttributeSet) :
			super(context, attrs)

	constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
			super(context, attrs, defStyleAttr)

	fun mark(points: List<Point>) {
		marks = points
		invalidate()
		removeCallbacks(invalidateRunnable)
		postDelayed(invalidateRunnable, 500)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent?): Boolean {
		event ?: return super.onTouchEvent(event)
		return when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				if (prefs.showCropHandle) {
					handleGrabbed = abs(event.x - handlePos.x) < handleXRadius &&
							abs(event.y - handlePos.y) < handleYRadius
					handleGrabbed
				} else {
					false
				}
			}
			MotionEvent.ACTION_MOVE -> {
				if (handleGrabbed) {
					handlePos.set(event.x, event.y)
					invalidate()
					true
				} else {
					false
				}
			}
			MotionEvent.ACTION_CANCEL -> {
				if (handleGrabbed) {
					snap(event.x, event.y)
					handleGrabbed = false
				}
				false
			}
			MotionEvent.ACTION_UP -> {
				if (handleGrabbed) {
					snap(event.x, event.y)
					updateRoi?.invoke()
					handleGrabbed = false
				}
				false
			}
			else -> super.onTouchEvent(event)
		}
	}

	private fun snap(x: Float, y: Float) {
		if (abs(x - center.x) < distToFull) {
			handlePos.x = center.x
			invalidate()
		}
		if (abs(y - center.y) < distToFull) {
			handlePos.y = center.y
			invalidate()
		}
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		val width = right - left
		val height = bottom - top
		center.set(
			(left + (width / 2)).toFloat(),
			(top + (height / 2)).toFloat()
		)
		if (width > height) {
			handlePos.set(round(right * .75f), center.y)
		} else {
			handlePos.set(center.x, round(bottom * .75f))
		}
	}

	override fun onDraw(canvas: Canvas) {
		canvas.drawColor(0, PorterDuff.Mode.CLEAR)
		updateClipRect()
		if (roi.height() > 0 && roi.width() > 0) {
			// canvas.clipRect() doesn't work reliably below KITKAT
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				canvas.save()
				canvas.clipOutRectCompat(roi)
				canvas.drawColor(shadeColor, PorterDuff.Mode.SRC)
				canvas.restore()
			} else {
				canvas.drawRect(roi, roiPaint)
			}
		}
		marks?.let {
			candidates.draw(canvas, it)
		}
		if (prefs.showCropHandle) {
			canvas.drawBitmap(
				handleBitmap,
				handlePos.x - handleXRadius,
				handlePos.y - handleYRadius,
				null
			)
		}
	}

	private fun updateClipRect() {
		val dx = abs(handlePos.x - center.x)
		val dy = abs(handlePos.y - center.y)
		val d = min(dx, dy)
		shadeColor = (min(1f, d / distToFull) * 128f).toInt() shl 24
		roi.set(
			(center.x - dx).roundToInt(),
			(center.y - dy).roundToInt(),
			(center.x + dx).roundToInt(),
			(center.y + dy).roundToInt()
		)
	}
}

private fun Canvas.clipOutRectCompat(rect: Rect) {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		clipOutRect(rect)
	} else {
		@Suppress("DEPRECATION")
		clipRect(rect, Region.Op.DIFFERENCE)
	}
}
