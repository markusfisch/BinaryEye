package de.markusfisch.android.binaryeye.decoder

import android.graphics.Rect
import de.markusfisch.android.zxingcpp.ZxingCpp.ReaderOptions
import de.markusfisch.android.zxingcpp.ZxingCpp.Result
import java.nio.ByteBuffer

/** No native dependency or additional work in ordinary builds. */
class DevelopmentDecoder : AutoCloseable {
	@Suppress("UNUSED_PARAMETER")
	fun read(buffer: ByteBuffer, stride: Int, crop: Rect, rotation: Int, options: ReaderOptions): Result? = null
	override fun close() {}
}
