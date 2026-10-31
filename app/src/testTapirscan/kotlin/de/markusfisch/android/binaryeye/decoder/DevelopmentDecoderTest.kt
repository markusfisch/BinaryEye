package de.markusfisch.android.binaryeye.decoder

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer
import org.tapirscan.android.Format
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat

@RunWith(RobolectricTestRunner::class)
class DevelopmentDecoderTest {
	@Test fun concreteFormatsMatchPreferences() {
		assertEquals(BarcodeFormat.Code39Std, toZxingFormat(Format.CODE39))
		assertEquals(BarcodeFormat.TelepenAlpha, toZxingFormat(Format.TELEPEN))
		assertEquals(BarcodeFormat.MicroPDF417, toZxingFormat(Format.MICRO_PDF417))
		assertEquals(BarcodeFormat.RMQRCode, toZxingFormat(Format.RM_QR_CODE))
	}
	@Test fun cropRespectsStridePositionAndRotation() {
		val buffer = ByteBuffer.wrap(byteArrayOf(99, 0, 1, 2, 9, 3, 4, 5, 9))
		buffer.position(1)
		val crop = Rect(1, 0, 3, 2)
		assertArrayEquals(byteArrayOf(1, 2, 4, 5), rotatedCrop(buffer, 4, crop, 0))
		assertArrayEquals(byteArrayOf(4, 1, 5, 2), rotatedCrop(buffer, 4, crop, 90))
		assertArrayEquals(byteArrayOf(5, 4, 2, 1), rotatedCrop(buffer, 4, crop, 180))
		assertArrayEquals(byteArrayOf(2, 5, 1, 4), rotatedCrop(buffer, 4, crop, 270))
		assertEquals(1, buffer.position())
	}

	@Test fun textualLinearReadsDoNotRequireOptionalBytes() {
		assertArrayEquals("TELEPEN".toByteArray(), plainTextBytes(Format.TELEPEN, "TELEPEN", null))
		assertArrayEquals("CODE39".toByteArray(), plainTextBytes(Format.CODE39, "CODE39", null))
		assertNull(plainTextBytes(Format.QR_CODE, "hello", null))
		assertNull(plainTextBytes(Format.TELEPEN, "\u0000", null))
		assertNull(plainTextBytes(Format.CODE128, "é", null))
	}

	@Test fun binaryDataIsNotPresentedAsText() {
		assertTrue(isPlainTextPayload("hello", "hello".toByteArray()))
		assertTrue(isPlainTextPayload("Grüße", "Grüße".toByteArray(Charsets.UTF_8)))
		assertFalse(isPlainTextPayload("�", byteArrayOf(0xff.toByte())))
		assertFalse(isPlainTextPayload("a\u0000b", byteArrayOf(97, 0, 98)))
	}
}
