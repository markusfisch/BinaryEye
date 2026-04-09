package de.markusfisch.android.binaryeye.activity

import android.graphics.Matrix
import android.graphics.Rect
import de.markusfisch.android.binaryeye.graphics.FrameMetrics
import de.markusfisch.android.binaryeye.graphics.setFrameRoi
import de.markusfisch.android.binaryeye.graphics.mapViewYToFrame
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
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

	@Test
	fun setFrameRoiClampsPartiallyVisibleRoiToFrameBounds() {
		val roi = Rect()
		roi.setFrameRoi(
			FrameMetrics(10, 12),
			Rect(0, 0, 10, 12),
			Rect(-3, 4, 20, 15)
		)
		assertEquals(Rect(0, 4, 10, 12), roi)
	}

	@Test
	fun setFrameRoiClearsFullyOutOfBoundsRoi() {
		val roi = Rect()
		roi.setFrameRoi(
			FrameMetrics(10, 12),
			Rect(0, 0, 10, 12),
			Rect(11, 4, 20, 15)
		)
		assertTrue(roi.isEmpty)
	}
}
