package de.markusfisch.android.binaryeye.rs

import de.markusfisch.android.binaryeye.renderscript.ScriptC_rotator

import android.content.Context
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.Element
import android.support.v8.renderscript.RenderScript
import android.support.v8.renderscript.ScriptIntrinsicResize
import android.support.v8.renderscript.Type

class Preprocessor(
	context: Context,
	width: Int,
	height: Int,
	val orientation: Int
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

		var f = .75f
		var w = Math.round(width * f)
		var h = Math.round(height * f)

		resizedType = Type.createXY(
			rs,
			Element.U8(rs),
			w,
			h
		)
		resizedAlloc = Allocation.createTyped(
			rs,
			resizedType,
			Allocation.USAGE_SCRIPT
		)

		if (orientation == 90 || orientation == 270) {
			val tmp = w
			w = h
			h = tmp
		}

		outWidth = w
		outHeight = h

		if (orientation != 0) {
			rotatedType = Type.createXY(
				rs,
				Element.U8(rs),
				w,
				h
			)
			rotatedAlloc = Allocation.createTyped(
				rs,
				rotatedType,
				Allocation.USAGE_SCRIPT
			)
		}
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

	fun process(frame: ByteArray) {
		yuvAlloc?.copyFrom(frame)

		resizeScript.setInput(yuvAlloc)
		resizeScript.forEach_bicubic(resizedAlloc)

		val t = resizedType
		if (t == null || orientation == 0) {
			resizedAlloc?.copyTo(frame)
		} else {
			rotatorScript._inImage = resizedAlloc
			rotatorScript._inWidth = t.x
			rotatorScript._inHeight = t.y
			when (orientation) {
				90 -> rotatorScript.forEach_rotate90(
					rotatedAlloc,
					rotatedAlloc
				)
				180 -> rotatorScript.forEach_rotate180(
					rotatedAlloc,
					rotatedAlloc
				)
				270 -> rotatorScript.forEach_rotate270(
					rotatedAlloc,
					rotatedAlloc
				)
			}
			rotatedAlloc?.copyTo(frame)
		}
	}
}
