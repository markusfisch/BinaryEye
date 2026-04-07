package de.markusfisch.android.binaryeye.activity

import android.graphics.Matrix
import de.markusfisch.android.binaryeye.graphics.mapViewYToFrame
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraActivityTest {
	@Test
	fun mapViewYToFrameInvertsScaleAndTranslate() {
		val m = Matrix()
		m.setScale(2f, 3f)
		m.postTranslate(0f, 100f)
		assertEquals(50f, m.mapViewYToFrame(250f))
	}
}
