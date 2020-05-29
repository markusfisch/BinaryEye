package de.markusfisch.android.binaryeye.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.graphics.Candidates
import de.markusfisch.android.binaryeye.graphics.getBitmapFromDrawable
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

class DetectorView : View {
	val roi = Rect()

	var updateRoi: (() -> Unit)? = null
	var handlePos = PointF()

	private val candidates = Candidates(context)
	private val invalidateRunnable: Runnable = Runnable {
		marks = null
		invalidate()
	}
	private val cropHandle = resources.getBitmapFromDrawable(
		R.drawable.ic_crop_handle
	)
	private val cropHandleXRadius = cropHandle.width / 2
	private val cropHandleYRadius = cropHandle.height / 2
	private val distToFull = 24f * context.resources.displayMetrics.density

	private var marks: List<Point>? = null
	private var axis = PointF()
	private var shadeColor = 0
	private var handleGrabbed = false

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
				handleGrabbed = abs(event.x - handlePos.x) < cropHandleXRadius &&
						abs(event.y - handlePos.y) < cropHandleYRadius
				handleGrabbed
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
		if (abs(x - axis.x) < distToFull) {
			handlePos.x = axis.x
			invalidate()
		}
		if (abs(y - axis.y) < distToFull) {
			handlePos.y = axis.y
			invalidate()
		}
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		axis.set(
			(left + ((right - left) / 2)).toFloat(),
			(top + ((bottom - top) / 2)).toFloat()
		)
		handlePos.set(
			round(right * .75f),
			axis.y
		)
	}

	override fun onDraw(canvas: Canvas) {
		canvas.drawColor(0, PorterDuff.Mode.CLEAR)
		updateClipRect()
		if (roi.height() > 0 && roi.width() > 0) {
			canvas.save()
			canvas.clipOutRectCompat(roi)
			canvas.drawColor(shadeColor, PorterDuff.Mode.SRC)
			canvas.restore()
		}
		marks?.let {
			candidates.draw(canvas, it)
		}
		canvas.drawBitmap(
			cropHandle,
			handlePos.x - cropHandleXRadius,
			handlePos.y - cropHandleYRadius,
			null
		)
	}

	private fun updateClipRect() {
		val dx = abs(handlePos.x - axis.x)
		val dy = abs(handlePos.y - axis.y)
		val d = min(dx, dy)
		shadeColor = (min(1f, d / distToFull) * 128f).toInt() shl 24
		roi.set(
			(axis.x - dx).roundToInt(),
			(axis.y - dy).roundToInt(),
			(axis.x + dx).roundToInt(),
			(axis.y + dy).roundToInt()
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
