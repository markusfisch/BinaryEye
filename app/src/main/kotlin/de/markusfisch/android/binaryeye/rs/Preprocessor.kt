package de.markusfisch.android.binaryeye.rs

import android.content.Context
import android.graphics.Rect
import android.support.v8.renderscript.*
import kotlin.math.max
import kotlin.math.roundToInt

private const val SCALE_FACTOR = .75f

class Preprocessor(
	context: Context,
	width: Int,
	height: Int,
	private val roi: Rect?
) {
	var outWidth = 0
	var outHeight = 0

	private val rs = RenderScript.create(context)
	private val resizeScript = ScriptIntrinsicResize.create(rs)
	private val rotateScript = ScriptC_rotate(rs)

	private var yuvType: Type? = null
	private var yuvAlloc: Allocation? = null
	private var roiType: Type? = null
	private var roiAlloc: Allocation? = null
	private var resizedType: Type? = null
	private var resizedAlloc: Allocation? = null
	private var rotatedType: Type? = null
	private var rotatedAlloc: Allocation? = null

	init {
		yuvType = Type.createXY(
			rs,
			Element.U8(rs),
			width,
			height // use only grayscale part
		)
		yuvAlloc = Allocation.createTyped(
			rs,
			yuvType,
			Allocation.USAGE_SCRIPT
		)

		if (roi != null) {
			val roiWidth = roi.width()
			val roiHeight = roi.height()

			roiType = Type.createXY(
				rs,
				Element.U8(rs),
				roiWidth,
				roiHeight
			)
			roiAlloc = Allocation.createTyped(
				rs,
				roiType,
				Allocation.USAGE_SCRIPT
			)

			outWidth = (roiWidth * SCALE_FACTOR).roundToInt()
			outHeight = (roiHeight * SCALE_FACTOR).roundToInt()

			// make sure the dimensions are always a multiple of 4
			outWidth -= outWidth % 4
			outHeight -= outHeight % 4

			// make sure the dimensions are always greater than 4
			outWidth = max(4, outWidth)
			outHeight = max(4, outHeight)
		} else {
			outWidth = (width * SCALE_FACTOR).roundToInt()
			outHeight = (height * SCALE_FACTOR).roundToInt()
		}

		resizedType = Type.createXY(
			rs,
			Element.U8(rs),
			outWidth,
			outHeight
		)
		resizedAlloc = Allocation.createTyped(
			rs,
			resizedType,
			Allocation.USAGE_SCRIPT
		)

		rotatedType = Type.createXY(
			rs,
			Element.U8(rs),
			outHeight,
			outWidth
		)
		rotatedAlloc = Allocation.createTyped(
			rs,
			rotatedType,
			Allocation.USAGE_SCRIPT
		)
	}

	fun destroy() {
		yuvType?.destroy()
		yuvType = null
		yuvAlloc?.destroy()
		yuvAlloc = null
		roiType?.destroy()
		roiType = null
		roiAlloc?.destroy()
		roiAlloc = null
		resizedType?.destroy()
		resizedType = null
		resizedAlloc?.destroy()
		resizedAlloc = null
		rotatedType?.destroy()
		rotatedType = null
		rotatedAlloc?.destroy()
		rotatedAlloc = null
		resizeScript.destroy()
		rotateScript.destroy()
		rs.destroy()
	}

	fun resizeOnly(frame: ByteArray) {
		resize(frame)
		resizedAlloc?.copyTo(frame)
	}

	fun resizeAndRotate(frame: ByteArray) {
		resize(frame)
		val t = resizedType ?: return
		rotateScript._inImage = resizedAlloc
		rotateScript._inWidth = t.x
		rotateScript._inHeight = t.y
		rotateScript.forEach_rotate90(
			rotatedAlloc, // ignored in kernel, just to satisfy forEach
			rotatedAlloc
		)
		rotatedAlloc?.copyTo(frame)
	}

	private fun resize(frame: ByteArray) {
		yuvAlloc?.copyFrom(frame)
		resizeScript.setInput(
			if (roi != null) {
				roiAlloc?.copy2DRangeFrom(
					0, 0,
					roi.width(), roi.height(),
					yuvAlloc,
					max(0, roi.left), max(0, roi.top)
				)
				roiAlloc
			} else {
				yuvAlloc
			}
		)
		resizeScript.forEach_bicubic(resizedAlloc)
	}
}
