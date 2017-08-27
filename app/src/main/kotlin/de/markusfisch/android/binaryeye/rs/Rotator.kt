package de.markusfisch.android.binaryeye.rs

import de.markusfisch.android.binaryeye.renderscript.ScriptC_rotator

import android.content.Context
import android.graphics.Bitmap
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.RenderScript

class Rotator(context: Context) {
	private val rs: RenderScript = RenderScript.create(context)
	private var rotatorScript: ScriptC_rotator = ScriptC_rotator(rs)

	fun destroy() {
		rotatorScript.destroy()
		rs.destroy()
	}

	fun convert(bitmap: Bitmap, frameOrientation: Int): Bitmap {
		var orientation = frameOrientation
		var width = bitmap.width
		var height = bitmap.height

		orientation = orientation % 360 / 90
		if (orientation == 0) {
			return bitmap
		}

		rotatorScript._inWidth = width
		rotatorScript._inHeight = height
		val sourceAllocation = Allocation.createFromBitmap(
				rs,
				bitmap,
				Allocation.MipmapControl.MIPMAP_NONE,
				Allocation.USAGE_SCRIPT)
		rotatorScript._inImage = sourceAllocation

		when (orientation) {
			1, 3 -> {
				val tmp = width
				width = height
				height = tmp
			}
		}

		val target = Bitmap.createBitmap(
				width,
				height,
				Bitmap.Config.ARGB_8888)
		val targetAllocation = Allocation.createFromBitmap(
				rs,
				target,
				Allocation.MipmapControl.MIPMAP_NONE,
				Allocation.USAGE_SCRIPT)

		when (orientation) {
			1 -> rotatorScript.forEach_rotate90(
					targetAllocation,
					targetAllocation)
			2 -> rotatorScript.forEach_rotate180(
					targetAllocation,
					targetAllocation)
			3 -> rotatorScript.forEach_rotate270(
					targetAllocation,
					targetAllocation)
		}

		targetAllocation.copyTo(target)
		return target
	}
}
