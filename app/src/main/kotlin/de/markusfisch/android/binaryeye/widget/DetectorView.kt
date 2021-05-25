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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class DetectorView : View {
	val currentOrientation = resources.configuration.orientation
	val roi = Rect()

	var onRoiChange: (() -> Unit)? = null
	var onRoiChanged: (() -> Unit)? = null

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
	private val handlePos = Point(-1, -1)
	private val handleHome = Point()
	private val center = Point()
	private val touchDown = Point()
	private val distToFull: Int
	private val minMoveThresholdSq: Int
	private val cornerRadius: Int
	private val fabHeight: Int
	private val padding: Int

	private var marks: List<Point>? = null
	private var handleGrabbed = false
	private var handleActive = false
	private var minY = 0
	private var maxY = 0
	private var minDist = 0
	private var shadeColor = 0

	init {
		val dp = context.resources.displayMetrics.density
		distToFull = (24f * dp).roundToInt()
		val minMoveThreshold = (8f * dp).roundToInt()
		minMoveThresholdSq = minMoveThreshold * minMoveThreshold
		cornerRadius = (8f * dp).roundToInt()
		fabHeight = (92f * dp).roundToInt()
		padding = (20f * dp).roundToInt()
		isSaveEnabled = true
	}

	constructor(context: Context, attrs: AttributeSet) :
			super(context, attrs)

	constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
			super(context, attrs, defStyleAttr)

	fun setCropHandlePos(x: Int, y: Int, orientation: Int) {
		if (orientation == currentOrientation) {
			handlePos.set(x, y)
		} else {
			handlePos.set(y, x)
		}
		if (x > -1) {
			handleActive = true
		}
	}

	fun getCropHandlePos() = if (handleActive) {
		handlePos
	} else {
		Point(-1, -1)
	}

	fun mark(points: List<Point>) {
		marks = points
		invalidate()
		removeCallbacks(invalidateRunnable)
		postDelayed(invalidateRunnable, 500)
	}

	override fun onSaveInstanceState(): Parcelable? {
		if (!handleActive) {
			return super.onSaveInstanceState()
		}
		return SavedState(super.onSaveInstanceState()).apply {
			savedHandlePos.set(getCropHandlePos())
			savedOrientation = currentOrientation
		}
	}

	override fun onRestoreInstanceState(state: Parcelable) {
		super.onRestoreInstanceState(
			if (state is SavedState) {
				setCropHandlePos(
					state.savedHandlePos.x,
					state.savedHandlePos.y,
					state.savedOrientation
				)
				state.superState
			} else {
				state
			}
		)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent?): Boolean {
		event ?: return super.onTouchEvent(event)
		val x = event.x.roundToInt()
		val y = event.y.roundToInt()
		return when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				if (prefs.showCropHandle) {
					touchDown.set(x, y)
					handleGrabbed = abs(x - handlePos.x) < handleXRadius &&
							abs(y - handlePos.y) < handleYRadius
					if (handleGrabbed) {
						onRoiChange?.invoke()
					}
					handleGrabbed
				} else {
					false
				}
			}
			MotionEvent.ACTION_MOVE -> {
				if (handleGrabbed) {
					handlePos.set(x, y)
					handleActive =
						handleActive or (distSq(handlePos, touchDown) > minMoveThresholdSq)
					if (handleActive) {
						updateClipRect()
						invalidate()
					}
					true
				} else {
					false
				}
			}
			MotionEvent.ACTION_CANCEL -> {
				if (handleGrabbed) {
					snap(x, y)
					handleGrabbed = false
				}
				false
			}
			MotionEvent.ACTION_UP -> {
				if (handleGrabbed) {
					if (!handleActive) {
						setHandleToDefaultRoi()
					} else {
						snap(x, y)
					}
					if (handleActive) {
						updateClipRect()
						invalidate()
					}
					onRoiChanged?.invoke()
					handleGrabbed = false
				}
				false
			}
			else -> super.onTouchEvent(event)
		}
	}

	private fun setHandleToDefaultRoi() {
		val mn = min(center.x, center.y) * .8f
		handlePos.set(
			(center.x + mn).roundToInt(),
			(center.y + mn).roundToInt()
		)
		handleActive = true
	}

	private fun snap(x: Int, y: Int) {
		val cx = clampX(x)
		val cy = clampY(y)
		val dx = abs(cx - center.x)
		val dy = abs(cy - center.y)
		// check if handle is close to the vertical or horizontal center line
		if (dx < distToFull ||
			dy < distToFull ||
			// check if handle is close to a screen corner
			((abs(cy - minY) < distToFull || abs(maxY - cy) < distToFull) &&
					abs(dx - center.x) < distToFull)
		) {
			reset()
			invalidate()
		}
	}

	private fun reset() {
		handlePos.set(handleHome)
		handleActive = false
		roi.set(0, 0, 0, 0)
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		val width = right - left
		val height = bottom - top
		center.set(
			left + (width / 2),
			top + (height / 2)
		)
		minY = padding * 2
		maxY = height - minY
		handleHome.set(
			width - handleXRadius - paddingRight - padding,
			height - handleYRadius - paddingBottom - fabHeight
		)
		if (handlePos.x == -2) {
			setHandleToDefaultRoi()
		}
		if (handleActive) {
			updateClipRect()
		} else {
			reset()
		}
	}

	override fun onDraw(canvas: Canvas) {
		if (handleActive) {
			drawClip(canvas)
		}
		marks?.let {
			dots.draw(canvas, it)
		}
		if (prefs.showCropHandle) {
			canvas.drawBitmap(
				handleBitmap,
				(handlePos.x - handleXRadius).toFloat(),
				(handlePos.y - handleYRadius).toFloat(),
				null
			)
		}
	}

	private fun drawClip(canvas: Canvas) {
		if (minDist < 1) {
			return
		}
		// canvas.clipRect() doesn't work reliably below KITKAT.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			val radius = min(minDist / 2, cornerRadius).toFloat()
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
			canvas.drawColor(shadeColor)
			canvas.restore()
		} else {
			canvas.drawRect(roi, roiPaint)
		}
	}

	private fun updateClipRect() {
		clampHandlePos()
		val dx = abs(handlePos.x - center.x)
		val dy = abs(handlePos.y - center.y)
		minDist = min(dx, dy)
		shadeColor = (min(
			1f,
			minDist.toFloat() / distToFull.toFloat()
		) * 128f).toInt() shl 24
		roi.set(
			center.x - dx,
			center.y - dy,
			center.x + dx,
			center.y + dy
		)
	}

	private fun clampHandlePos() {
		handlePos.x = clampX(handlePos.x)
		handlePos.y = clampY(handlePos.y)
	}

	private fun clampX(x: Int) = min(center.x * 2, max(0, x))

	private fun clampY(y: Int) = min(maxY, max(minY, y))

	internal class SavedState : BaseSavedState {
		val savedHandlePos = Point()

		var savedOrientation = 0

		constructor(superState: Parcelable?) : super(superState)

		private constructor(parcel: Parcel) : super(parcel) {
			savedHandlePos.set(
				parcel.readInt(),
				parcel.readInt()
			)
			savedOrientation = parcel.readInt()
		}

		override fun writeToParcel(out: Parcel, flags: Int) {
			super.writeToParcel(out, flags)
			out.writeInt(savedHandlePos.x)
			out.writeInt(savedHandlePos.y)
			out.writeInt(savedOrientation)
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

private fun distSq(a: Point, b: Point): Int {
	val dx = a.x - b.x
	val dy = a.y - b.y
	return dx * dx + dy * dy
}

private fun Point.set(point: Point) {
	x = point.x
	y = point.y
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
