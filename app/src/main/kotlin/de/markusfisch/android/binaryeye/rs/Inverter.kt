package de.markusfisch.android.binaryeye.rs

import de.markusfisch.android.binaryeye.renderscript.ScriptC_invert

import android.content.Context
import android.graphics.Bitmap
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.RenderScript

class Inverter(context: Context) {
	private val rs: RenderScript = RenderScript.create(context)
	private val inverterScript: ScriptC_invert = ScriptC_invert(rs)

	fun destroy() {
		inverterScript.destroy()
		rs.destroy()
	}

	fun convert(bitmap: Bitmap): Bitmap {
		val allocation = Allocation.createFromBitmap(
				rs,
				bitmap,
				Allocation.MipmapControl.MIPMAP_NONE,
				Allocation.USAGE_SCRIPT)

		inverterScript.forEach_invert(allocation, allocation)
		allocation.copyTo(bitmap)

		return bitmap
	}
}
