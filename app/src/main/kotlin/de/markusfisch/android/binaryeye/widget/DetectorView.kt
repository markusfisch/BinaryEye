package de.markusfisch.android.binaryeye.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.graphics.Dots
import de.markusfisch.android.binaryeye.graphics.getBitmapFromDrawable
import de.markusfisch.android.binaryeye.graphics.getDashedBorderPaint
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class DetectorView : View {
	val roi = Rect()

	var updateRoi: (() -> Unit)? = null

	private val dots = Dots(context)
	private val invalidateRunnable: Runnable = Runnable {
		marks = null
		invalidate()
	}
	private val roiPaint = context.getDashedBorderPaint()
	private val handleBitmap = resources.getBitmapFromDrawable(
		R.drawable.button_crop
	)
	private val handleXRadius = handleBitmap.width / 2
	private val handleYRadius = handleBitmap.height / 2
	private val handlePos = PointF(-1f, -1f)
	private val center = PointF()
	private val touchDown = PointF()
	private val distToFull: Float
	private val minMoveThresholdSq: Float
	private val cornerRadius: Float
	private val fabHeight: Float
	private val padding: Float

	private var marks: List<Point>? = null
	private var orientation = resources.configuration.orientation
	private var handleGrabbed = false
	private var shadeColor = 0
	private var movedHandle = false

	init {
		val dp = context.resources.displayMetrics.density
		distToFull = 24f * dp
		val minMoveThreshold = 8f * dp
		minMoveThresholdSq = minMoveThreshold * minMoveThreshold
		cornerRadius = 8f * dp
		fabHeight = (72f + 20f) * dp
		padding = 20f * dp
		isSaveEnabled = true
	}

	constructor(context: Context, attrs: AttributeSet) :
			super(context, attrs)

	constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
			super(context, attrs, defStyleAttr)

	override fun onSaveInstanceState(): Parcelable? {
		if (!movedHandle) {
			return super.onSaveInstanceState()
		}
		return SavedState(super.onSaveInstanceState()).apply {
			handlePos.set(this@DetectorView.handlePos)
			orientation = this@DetectorView.orientation
		}
	}

	override fun onRestoreInstanceState(state: Parcelable) {
		super.onRestoreInstanceState(
			if (state is SavedState) {
				if (state.orientation == orientation) {
					handlePos.set(state.handlePos)
				} else {
					handlePos.set(
						state.handlePos.y,
						state.handlePos.x
					)
				}
				movedHandle = true
				state.superState
			} else {
				state
			}
		)
	}

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
					touchDown.set(event.x, event.y)
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
					if (distSq(handlePos, touchDown) > minMoveThresholdSq) {
						movedHandle = true
					}
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
					if (!movedHandle) {
						handlePos.set(center.x * 1.75f, center.y * 1.25f)
						movedHandle = true
						invalidate()
					} else {
						snap(event.x, event.y)
					}
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
			movedHandle = false
		}
		if (abs(y - center.y) < distToFull) {
			handlePos.y = center.y
			invalidate()
			movedHandle = false
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
		if (handlePos.x < 0) {
			handlePos.set(
				width - handleXRadius - paddingRight - padding,
				height - handleYRadius - paddingBottom - fabHeight
			)
		}
	}

	override fun onDraw(canvas: Canvas) {
		canvas.drawColor(0, PorterDuff.Mode.CLEAR)
		if (movedHandle) {
			val minDist = updateClipRect()
			if (roi.height() > 0 && roi.width() > 0) {
				// canvas.clipRect() doesn't work reliably below KITKAT
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					val radius = min(minDist * .5f, cornerRadius)
					canvas.save()
					canvas.clipOutPathCompat(
						calculateRoundedRectPath(
							roi.left.toFloat(),
							roi.top.toFloat(),
							roi.right.toFloat(),
							roi.bottom.toFloat(),
							radius,
							radius
						)
					)
					canvas.drawColor(shadeColor, PorterDuff.Mode.SRC)
					canvas.restore()
				} else {
					canvas.drawRect(roi, roiPaint)
				}
			}
		}
		marks?.let {
			dots.draw(canvas, it)
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

	private fun updateClipRect(): Float {
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
		return d
	}

	internal class SavedState : BaseSavedState {
		val handlePos = PointF()

		var orientation = 0

		constructor(superState: Parcelable?) : super(superState)

		private constructor(parcel: Parcel) : super(parcel) {
			handlePos.set(
				parcel.readFloat(),
				parcel.readFloat()
			)
			orientation = parcel.readInt()
		}

		override fun writeToParcel(out: Parcel, flags: Int) {
			super.writeToParcel(out, flags)
			out.writeFloat(handlePos.x)
			out.writeFloat(handlePos.y)
			out.writeInt(orientation)
		}

		companion object {
			@JvmField
			val CREATOR = object : Parcelable.Creator<SavedState> {
				override fun createFromParcel(source: Parcel) = SavedState(source)
				override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
			}
		}
	}
}

private fun distSq(a: PointF, b: PointF): Float {
	val dx = a.x - b.x
	val dy = a.y - b.y
	return dx * dx + dy * dy
}

private fun Canvas.clipOutPathCompat(path: Path) {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		clipOutPath(path)
	} else {
		@Suppress("DEPRECATION")
		clipPath(path, Region.Op.DIFFERENCE)
	}
}

private fun calculateRoundedRectPath(
	left: Float,
	top: Float,
	right: Float,
	bottom: Float,
	rx: Float,
	ry: Float
): Path {
	val width = right - left
	val height = bottom - top
	val widthMinusCorners = width - 2 * rx
	val heightMinusCorners = height - 2 * ry
	return Path().apply {
		moveTo(right, top + ry)
		rQuadTo(0f, -ry, -rx, -ry)
		rLineTo(-widthMinusCorners, 0f)
		rQuadTo(-rx, 0f, -rx, ry)
		rLineTo(0f, heightMinusCorners)
		rQuadTo(0f, ry, rx, ry)
		rLineTo(widthMinusCorners, 0f)
		rQuadTo(rx, 0f, rx, -ry)
		rLineTo(0f, -heightMinusCorners)
		close()
	}
}
