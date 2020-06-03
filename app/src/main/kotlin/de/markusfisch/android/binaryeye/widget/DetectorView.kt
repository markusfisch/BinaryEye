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
	private val handleHome = Point()
	private val handlePos = Point(-1, -1)
	private val center = Point()
	private val touchDown = Point()
	private val distToFull: Int
	private val minMoveThresholdSq: Int
	private val cornerRadius: Int
	private val fabHeight: Int
	private val padding: Int

	private var marks: List<Point>? = null
	private var orientation = resources.configuration.orientation
	private var handleGrabbed = false
	private var handleMoved = false
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

	fun mark(points: List<Point>) {
		marks = points
		invalidate()
		removeCallbacks(invalidateRunnable)
		postDelayed(invalidateRunnable, 500)
	}

	override fun onSaveInstanceState(): Parcelable? {
		if (!handleMoved) {
			return super.onSaveInstanceState()
		}
		return SavedState(super.onSaveInstanceState()).apply {
			savedHandlePos.set(handlePos)
			savedOrientation = orientation
		}
	}

	override fun onRestoreInstanceState(state: Parcelable) {
		super.onRestoreInstanceState(
			if (state is SavedState) {
				if (state.savedOrientation == orientation) {
					handlePos.set(state.savedHandlePos)
				} else {
					handlePos.set(
						state.savedHandlePos.y,
						state.savedHandlePos.x
					)
				}
				handleMoved = true
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
					if (distSq(handlePos, touchDown) > minMoveThresholdSq) {
						handleMoved = true
					}
					invalidate()
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
					if (!handleMoved) {
						handlePos.set(
							(center.x * 1.5f).roundToInt(),
							(center.y * 1.25f).roundToInt()
						)
						handleMoved = true
						invalidate()
					} else {
						snap(x, y)
					}
					onRoiChanged?.invoke()
					handleGrabbed = false
				}
				false
			}
			else -> super.onTouchEvent(event)
		}
	}

	private fun snap(x: Int, y: Int) {
		if (abs(x - center.x) < distToFull ||
			abs(y - center.y) < distToFull
		) {
			handlePos.set(handleHome)
			handleMoved = false
			invalidate()
		}
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		val width = right - left
		val height = bottom - top
		center.set(
			left + (width / 2),
			top + (height / 2)
		)
		handleHome.set(
			width - handleXRadius - paddingRight - padding,
			height - handleYRadius - paddingBottom - fabHeight
		)
		if (handlePos.x < 0) {
			handlePos.set(handleHome)
		}
	}

	override fun onDraw(canvas: Canvas) {
		canvas.drawColor(0, PorterDuff.Mode.CLEAR)
		if (handleMoved) {
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
		val minDist = updateClipRect()
		if (minDist < 1) {
			return
		}
		// canvas.clipRect() doesn't work reliably below KITKAT
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
			canvas.drawColor(shadeColor, PorterDuff.Mode.SRC)
			canvas.restore()
		} else {
			canvas.drawRect(roi, roiPaint)
		}
	}

	private fun updateClipRect(): Int {
		val dx = abs(handlePos.x - center.x)
		val dy = abs(handlePos.y - center.y)
		val d = min(dx, dy)
		shadeColor = (min(
			1f,
			d.toFloat() / distToFull.toFloat()
		) * 128f).toInt() shl 24
		roi.set(
			center.x - dx,
			center.y - dy,
			center.x + dx,
			center.y + dy
		)
		return d
	}

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
