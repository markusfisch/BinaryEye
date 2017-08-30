package de.markusfisch.android.binaryeye.widget

import de.markusfisch.android.binaryeye.view.SystemBarMetrics
import de.markusfisch.android.binaryeye.R

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.SurfaceHolder

class LockOnView : SurfaceView {
	private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val restRect = Rect()
	private val startRect = Rect()
	private val endRect = Rect()
	private val currentRect = Rect()
	private val animationRunnable = Runnable {
		setAnimationStart()
		while (running) {
			val now = System.currentTimeMillis() - animationStart
			if (now > animationDuration) {
				break
			}
			animateCurrentRect(now)
			lockCanvasAndDraw()
		}
		lockCanvasAndDraw()
		running = false
	}

	@Volatile private var running = false

	private lateinit var markerTopLeft: Bitmap
	private lateinit var markerTopRight: Bitmap
	private lateinit var markerBottomLeft: Bitmap
	private lateinit var markerBottomRight: Bitmap

	private var markerSize: Int = 0
	private var minSize: Int = 0
	private var thread: Thread? = null
	private var animationStart: Long = 0
	private var animationDuration: Long = 0

	init {
		loadMarkers(context.resources)
		initSurfaceHolder(context)
		setZOrderOnTop(true)
	}

	constructor(context: Context, attrs: AttributeSet) :
			super(context, attrs) {
	}

	constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
			super(context, attrs, defStyleAttr) {
	}

	fun lock(rect: Rect, ms: Int) {
		startRect.set(currentRect)
		if (running) {
			tintMarkers()
			fitRect(rect)
			endRect.set(rect)
			setAnimationStart()
		} else {
			startAnimation(rect, ms)
		}
	}

	fun reset() {
		untintMarkers()
		startRect.set(restRect)
		currentRect.set(startRect)
		lockCanvasAndDraw()
	}

	private fun loadMarkers(res: Resources) {
		val dp = res.displayMetrics.density
		markerTopLeft = BitmapFactory.decodeResource(res, R.drawable.marker)
		val width = markerTopLeft.width
		val height = markerTopLeft.height
		markerSize = Math.max(width, height)
		minSize = markerSize * 2 + Math.round(dp * 8f)
		val matrix = Matrix()
		matrix.setScale(-1f, 1f)
		markerTopRight = Bitmap.createBitmap(markerTopLeft,
				0, 0, width, height, matrix, false);
		matrix.setScale(1f, -1f)
		markerBottomLeft = Bitmap.createBitmap(markerTopLeft,
				0, 0, width, height, matrix, false);
		matrix.setScale(-1f, -1f)
		markerBottomRight = Bitmap.createBitmap(markerTopLeft,
				0, 0, width, height, matrix, false);
	}

	private fun initSurfaceHolder(context: Context) {
		holder.setFormat(PixelFormat.TRANSPARENT)
		holder.addCallback(object : SurfaceHolder.Callback {
			override fun surfaceChanged(
					holder: SurfaceHolder,
					format: Int,
					width: Int,
					height: Int) {
				initRestRect(context, width, height)
				reset()
			}

			override fun surfaceCreated(holder: SurfaceHolder) {}

			override fun surfaceDestroyed(holder: SurfaceHolder) {
				cancelAnimation()
			}
		})
	}

	private fun cancelAnimation() {
		running = false
		if (thread != null) {
			var retry = 100
			while (retry-- > 0) {
				try {
					thread?.join()
					retry = 0
				} catch (e: InterruptedException) {
					// try again
				}
			}
		}
	}

	private fun startAnimation(rect: Rect, ms: Int) {
		if (running) {
			return
		}
		if (rect == restRect) {
			untintMarkers()
		} else {
			tintMarkers()
		}
		running = true
		fitRect(rect)
		endRect.set(rect)
		animationDuration = ms.toLong()
		thread = Thread(animationRunnable)
		thread?.start()
	}

	private fun initRestRect(context: Context, width: Int, height: Int) {
		val res = context.resources
		val dp = res.displayMetrics.density
		val padding = Math.round(16f * dp)
		var left = padding
		var top = padding + if (width > height)
			SystemBarMetrics.getStatusAndToolBarHeight(context)
		else
			0
		val paddingLeftRight = padding * 2
		val paddingTopBottom = top + padding
		val innerWidth = width - paddingLeftRight
		val innerHeight = height - paddingTopBottom
		val size = Math.min(innerWidth, innerHeight)
		left += (innerWidth - size) / 2
		top += (innerHeight - size) / 2
		restRect.set(left, top, left + size, top + size)
	}

	private fun fitRect(rect: Rect) {
		val width = rect.width()
		val height = rect.height()
		if (width < minSize) {
			val pad = (minSize - width) / 2
			rect.left -= pad
			rect.right += pad
		}
		if (height < minSize) {
			val pad = (minSize - height) / 2
			rect.top -= pad
			rect.bottom += pad
		}
	}

	private fun setAnimationStart() {
		animationStart = System.currentTimeMillis()
	}

	private fun animateCurrentRect(now: Long) {
		currentRect.set(
				linear(now,
						startRect.left,
						endRect.left - startRect.left,
						animationDuration),
				linear(now,
						startRect.top,
						endRect.top - startRect.top,
						animationDuration),
				linear(now,
						startRect.right,
						endRect.right - startRect.right,
						animationDuration),
				linear(now,
						startRect.bottom,
						endRect.bottom - startRect.bottom,
						animationDuration))
	}

	private fun linear(
			time: Long,
			begin: Int,
			change: Int,
			duration: Long): Int = Math.round(
					change.toFloat() * time / duration + begin)

	private fun lockCanvasAndDraw() {
		val canvas = holder.lockCanvas()
		if (canvas != null) {
			drawMarkers(canvas, currentRect)
			holder.unlockCanvasAndPost(canvas)
		}
	}

	private fun untintMarkers() {
		markerPaint.colorFilter = null
	}

	private fun tintMarkers() {
		markerPaint.colorFilter = PorterDuffColorFilter(
				ContextCompat.getColor(context, R.color.accent),
				PorterDuff.Mode.SRC_IN)
	}

	private fun drawMarkers(canvas: Canvas, rect: Rect) {
		canvas.drawColor(0, PorterDuff.Mode.CLEAR)
		canvas.drawBitmap(markerTopLeft,
				rect.left.toFloat(),
				rect.top.toFloat(),
				markerPaint)
		canvas.drawBitmap(markerTopRight,
				(rect.right - markerSize).toFloat(),
				rect.top.toFloat(),
				markerPaint)
		canvas.drawBitmap(markerBottomLeft,
				rect.left.toFloat(),
				(rect.bottom - markerSize).toFloat(),
				markerPaint)
		canvas.drawBitmap(markerBottomRight,
				(rect.right - markerSize).toFloat(),
				(rect.bottom - markerSize).toFloat(),
				markerPaint)
	}
}
