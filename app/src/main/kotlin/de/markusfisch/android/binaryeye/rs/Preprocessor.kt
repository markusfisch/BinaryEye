package de.markusfisch.android.binaryeye.rs

import de.markusfisch.android.binaryeye.renderscript.ScriptC_rotator
import de.markusfisch.android.binaryeye.renderscript.ScriptC_yuv2gray

import android.content.Context
import android.graphics.Bitmap
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.Element
import android.support.v8.renderscript.RenderScript
import android.support.v8.renderscript.Type

class Preprocessor(context: Context) {
	private val rs: RenderScript = RenderScript.create(context)
	private val rotatorScript: ScriptC_rotator = ScriptC_rotator(rs)
	private val yuv2grayScript: ScriptC_yuv2gray = ScriptC_yuv2gray(rs)

	private var yuvType: Type? = null
	private var rgbaType: Type? = null
	private var yuvAlloc: Allocation? = null
	private var rgbaAlloc: Allocation? = null
	private var destAlloc: Allocation? = null
	private var dest: Bitmap? = null
	private var odd = true

	fun destroy() {
		yuvType?.destroy()
		yuvType = null
		rgbaType?.destroy()
		rgbaType = null
		yuvAlloc?.destroy()
		yuvAlloc = null
		rgbaAlloc?.destroy()
		rgbaAlloc = null
		destAlloc?.destroy()
		destAlloc = null
		dest?.recycle()
		dest = null
		rotatorScript.destroy()
		yuv2grayScript.destroy()
		rs.destroy()
	}

	fun convert(
		data: ByteArray,
		width: Int,
		height: Int,
		orientation: Int
	): Bitmap {
		if (dest == null) {
			yuvType = Type.createXY(rs, Element.U8(rs), width, height * 3 / 2)
			yuvAlloc = Allocation.createTyped(
				rs, yuvType,
				Allocation.USAGE_SCRIPT
			)
			rgbaType = Type.createXY(rs, Element.RGBA_8888(rs), width, height)
			rgbaAlloc = Allocation.createTyped(
				rs, rgbaType,
				Allocation.USAGE_SCRIPT
			)

			var w = width
			var h = height
			when (orientation) {
				90, 270 -> {
					val tmp = w
					w = h
					h = tmp
				}
			}

			dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
			destAlloc = Allocation.createFromBitmap(
				rs,
				dest,
				Allocation.MipmapControl.MIPMAP_NONE,
				Allocation.USAGE_SCRIPT
			)
		}

		yuvAlloc?.copyFrom(data)
		yuv2grayScript._inYUV = yuvAlloc
		// width/height are uint_32 but Kotlin wants toLong()
		yuv2grayScript._inWidth = width.toLong()
		yuv2grayScript._inHeight = height.toLong()

		// invert every second frame to also read inverted barcodes
		if (odd) {
			yuv2grayScript.forEach_yuv2gray(rgbaAlloc)
		} else {
			yuv2grayScript.forEach_yuv2inverted(rgbaAlloc)
		}
		odd = odd xor true

		rotatorScript._inImage = rgbaAlloc
		rotatorScript._inWidth = width
		rotatorScript._inHeight = height

		when (orientation) {
			0 -> destAlloc?.copyFrom(rgbaAlloc)
			90 -> rotatorScript.forEach_rotate90(destAlloc, destAlloc)
			180 -> rotatorScript.forEach_rotate180(destAlloc, destAlloc)
			270 -> rotatorScript.forEach_rotate270(destAlloc, destAlloc)
		}

		destAlloc?.copyTo(dest)

		// since Bitmap.createBitmap() can't return null,
		// dest cannot be null here either
		return dest!!
	}
}
