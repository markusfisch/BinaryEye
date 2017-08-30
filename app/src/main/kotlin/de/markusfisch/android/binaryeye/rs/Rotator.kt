package de.markusfisch.android.binaryeye.rs

import de.markusfisch.android.binaryeye.renderscript.ScriptC_rotator

import android.content.Context
import android.graphics.Bitmap
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.RenderScript

class Rotator(context: Context) {
	private val rs: RenderScript = RenderScript.create(context)
	private val rotatorScript: ScriptC_rotator = ScriptC_rotator(rs)

	private var sourceAlloc: Allocation? = null
	private var destAlloc: Allocation? = null
	private var dest: Bitmap? = null

	fun destroy() {
		sourceAlloc?.destroy()
		sourceAlloc = null
		destAlloc?.destroy()
		destAlloc = null
		dest?.recycle()
		dest = null
		rotatorScript.destroy()
		rs.destroy()
	}

	fun convert(bitmap: Bitmap, frameOrientation: Int): Bitmap {
		var orientation = frameOrientation

		orientation = orientation % 360 / 90
		if (orientation == 0) {
			return bitmap
		}

		if (dest == null) {
			var width = bitmap.width
			var height = bitmap.height

			sourceAlloc = Allocation.createFromBitmap(
					rs,
					bitmap,
					Allocation.MipmapControl.MIPMAP_NONE,
					Allocation.USAGE_SCRIPT)
			rotatorScript._inImage = sourceAlloc
			rotatorScript._inWidth = width
			rotatorScript._inHeight = height

			when (orientation) {
				1, 3 -> {
					val tmp = width
					width = height
					height = tmp
				}
			}

			dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
			destAlloc = Allocation.createFromBitmap(
					rs,
					dest,
					Allocation.MipmapControl.MIPMAP_NONE,
					Allocation.USAGE_SCRIPT)
		} else {
			sourceAlloc?.copyFrom(bitmap)
		}

		when (orientation) {
			1 -> rotatorScript.forEach_rotate90(destAlloc, destAlloc)
			2 -> rotatorScript.forEach_rotate180(destAlloc, destAlloc)
			3 -> rotatorScript.forEach_rotate270(destAlloc, destAlloc)
		}
		destAlloc?.copyTo(dest)

		return dest!!
	}
}
