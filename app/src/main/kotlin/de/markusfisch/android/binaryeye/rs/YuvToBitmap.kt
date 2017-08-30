package de.markusfisch.android.binaryeye.rs

import android.content.Context
import android.graphics.Bitmap
import android.support.v8.renderscript.Allocation
import android.support.v8.renderscript.Element
import android.support.v8.renderscript.RenderScript
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB
import android.support.v8.renderscript.Type

class YuvToBitmap(context: Context) {
	private val rs: RenderScript = RenderScript.create(context)

	private var yuvType: Type? = null
	private var rgbaType: Type? = null
	private var yuvAllocation: Allocation? = null
	private var rgbaAllocation: Allocation? = null
	private var yuvToRgbaScript: ScriptIntrinsicYuvToRGB? = null
	private var dest: Bitmap? = null

	fun destroy() {
		yuvToRgbaScript?.destroy()
		yuvToRgbaScript = null
		yuvType?.destroy()
		yuvType = null
		rgbaType?.destroy()
		rgbaType = null
		yuvAllocation?.destroy()
		yuvAllocation = null
		rgbaAllocation?.destroy()
		rgbaAllocation = null
		dest?.recycle()
		dest = null
		rs.destroy()
	}

	fun convert(data: ByteArray, width: Int, height: Int): Bitmap {
		if (dest == null) {
			yuvType = Type.createX(rs, Element.U8(rs), data.size)
			yuvType?.let {
				yuvAllocation = Allocation.createTyped(rs, yuvType,
						Allocation.USAGE_SCRIPT)
			}

			rgbaType = Type.createXY(rs, Element.RGBA_8888(rs), width, height)
			rgbaType?.let {
				rgbaAllocation = Allocation.createTyped(rs, rgbaType)
			}

			yuvToRgbaScript = ScriptIntrinsicYuvToRGB.create(rs,
					Element.U8_4(rs))
			yuvToRgbaScript?.setInput(yuvAllocation)

			dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		}

		yuvAllocation?.copyFrom(data)
		yuvToRgbaScript?.forEach(rgbaAllocation)
		rgbaAllocation?.copyTo(dest)

		return dest!!
	}
}
