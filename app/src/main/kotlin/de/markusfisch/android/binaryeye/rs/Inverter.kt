package de.markusfisch.android.binaryeye.rs

import de.markusfisch.android.binaryeye.renderscript.ScriptC_invert

import android.content.Context
import android.graphics.Bitmap
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.RenderScript

class Inverter(context: Context) {
	private val rs: RenderScript = RenderScript.create(context)
	private val inverterScript: ScriptC_invert = ScriptC_invert(rs)

	private var alloc: Allocation? = null
	private var dest: Bitmap? = null

	fun destroy() {
		alloc?.destroy()
		alloc = null
		dest?.recycle()
		dest = null
		inverterScript.destroy()
		rs.destroy()
	}

	fun convert(bitmap: Bitmap): Bitmap {
		if (dest == null) {
			dest = bitmap.copy(bitmap.getConfig(), true)
			alloc = Allocation.createFromBitmap(
					rs,
					dest,
					Allocation.MipmapControl.MIPMAP_NONE,
					Allocation.USAGE_SCRIPT)
		}

		inverterScript.forEach_invert(alloc, alloc)
		alloc?.copyTo(dest)

		return dest!!
	}
}
