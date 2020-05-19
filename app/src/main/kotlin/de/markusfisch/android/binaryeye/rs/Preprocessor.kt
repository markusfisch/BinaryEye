package de.markusfisch.android.binaryeye.rs

import android.content.Context
import android.support.v8.renderscript.*
import de.markusfisch.android.binaryeye.renderscript.ScriptC_rotator
import kotlin.math.roundToInt

private const val SCALE_FACTOR = .75f

class Preprocessor(
	context: Context,
	width: Int,
	height: Int
) {
	var outWidth = 0
	var outHeight = 0

	private val rs = RenderScript.create(context)
	private val resizeScript = ScriptIntrinsicResize.create(rs)
	private val rotatorScript = ScriptC_rotator(rs)

	private var yuvType: Type? = null
	private var yuvAlloc: Allocation? = null
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

		outWidth = (width * SCALE_FACTOR).roundToInt()
		outHeight = (height * SCALE_FACTOR).roundToInt()

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
		resizedType?.destroy()
		resizedType = null
		resizedAlloc?.destroy()
		resizedAlloc = null
		rotatedType?.destroy()
		rotatedType = null
		rotatedAlloc?.destroy()
		rotatedAlloc = null
		resizeScript.destroy()
		rotatorScript.destroy()
		rs.destroy()
	}

	fun resizeOnly(frame: ByteArray) {
		resize(frame)
		resizedAlloc?.copyTo(frame)
	}

	fun resizeAndRotate(frame: ByteArray) {
		resize(frame)
		val t = resizedType ?: return
		rotatorScript._inImage = resizedAlloc
		rotatorScript._inWidth = t.x
		rotatorScript._inHeight = t.y
		rotatorScript.forEach_rotate90(
			rotatedAlloc, // ignored in kernel, just to satisfy forEach
			rotatedAlloc
		)
		rotatedAlloc?.copyTo(frame)
	}

	private fun resize(frame: ByteArray) {
		yuvAlloc?.copyFrom(frame)
		resizeScript.setInput(yuvAlloc)
		resizeScript.forEach_bicubic(resizedAlloc)
	}
}
