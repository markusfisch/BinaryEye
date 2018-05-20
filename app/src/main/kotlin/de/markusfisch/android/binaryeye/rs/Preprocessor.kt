package de.markusfisch.android.binaryeye.rs

import de.markusfisch.android.binaryeye.renderscript.ScriptC_rotator

import android.content.Context
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.Element
import android.support.v8.renderscript.RenderScript
import android.support.v8.renderscript.Type

class Preprocessor(context: Context) {
	private val rs: RenderScript = RenderScript.create(context)
	private val rotatorScript: ScriptC_rotator = ScriptC_rotator(rs)

	private var yuvType: Type? = null
	private var yuvAlloc: Allocation? = null
	private var destType: Type? = null
	private var destAlloc: Allocation? = null

	fun destroy() {
		yuvType?.destroy()
		yuvType = null
		yuvAlloc?.destroy()
		yuvAlloc = null
		destType?.destroy()
		destType = null
		yuvAlloc?.destroy()
		destAlloc?.destroy()
		destAlloc = null
		rotatorScript.destroy()
		rs.destroy()
	}

	fun convert(
		data: ByteArray,
		width: Int,
		height: Int,
		orientation: Int
	) {
		if (orientation == 0) {
			return
		}

		if (yuvAlloc == null) {
			yuvType = Type.createXY(
				rs,
				Element.U8(rs),
				width,
				height * 3 / 2
			)
			yuvAlloc = Allocation.createTyped(
				rs,
				yuvType,
				Allocation.USAGE_SCRIPT
			)

			var w = width
			var h = height
			if (orientation == 90 || orientation == 270) {
				val tmp = w
				w = h
				h = tmp
			}

			destType = Type.createXY(
				rs,
				Element.U8(rs),
				w,
				h
			)
			destAlloc = Allocation.createTyped(
				rs,
				destType,
				Allocation.USAGE_SCRIPT
			)
		}

		yuvAlloc?.copyFrom(data)
		rotatorScript._inImage = yuvAlloc
		rotatorScript._inWidth = width
		rotatorScript._inHeight = height

		when (orientation) {
			90 -> rotatorScript.forEach_rotate90(destAlloc, destAlloc)
			180 -> rotatorScript.forEach_rotate180(destAlloc, destAlloc)
			270 -> rotatorScript.forEach_rotate270(destAlloc, destAlloc)
		}

		destAlloc?.copyTo(data)
	}
}
