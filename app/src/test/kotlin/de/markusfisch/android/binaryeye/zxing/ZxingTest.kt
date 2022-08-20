package de.markusfisch.android.binaryeye.zxing

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.util.*

class ZxingTest {
	@Test
	fun encodeAsTextDataMatrix() {
		val result = encodeAsText("4102560010270", BarcodeFormat.DATA_MATRIX)
		assertEquals(
			"""▀█▀███▀█▀█▀▀██
▀   ▀▄▀▄▀ ▄▄ █
▀▀▀ ▀▀▀▄▀▀█▀ █
▀ ▄▀▄ ████▀▄██
▀ ▄▄▄▄ ▄ █ ▄██
▀ ▀▀██▄▄▄▀▄█▀█
▀▄ ▄▀▄▀▄ █ █ █
""", result
		)
	}

	@Test
	fun encodeAsTextQR() {
		val result = encodeAsText(
			"Binary Eye",
			BarcodeFormat.QR_CODE,
			EnumMap(mapOf(EncodeHintType.MARGIN to 0))
		)
		assertEquals(
			"█▀▀▀▀▀█ ██▀▀█ █▀▀▀▀▀█\n" +
					"█ ███ █ █▄▀█  █ ███ █\n" +
					"█ ▀▀▀ █ ▀ ▄▀  █ ▀▀▀ █\n" +
					"▀▀▀▀▀▀▀ █▄▀ █ ▀▀▀▀▀▀▀\n" +
					"▄▀▄▀ █▀ █ █▀ ██ █▀▀▀▀\n" +
					" ▄ ▄█▀▀█ ▀▄▀█▀▀  ▄▄▀▄\n" +
					"▀   ▀▀▀▀█▀   █▀████ █\n" +
					"█▀▀▀▀▀█ ▄ █  ▄▄   ██ \n" +
					"█ ███ █ ▄▄▀▀ █ ▀▄▄ █ \n" +
					"█ ▀▀▀ █ ▄▀▀▀ ▀▄ ▀█ ▄▄\n" +
					"▀▀▀▀▀▀▀  ▀▀    ▀▀  ▀ \n",
			result
		)
	}

	@Test
	fun encodeAsTextQRInverted() {
		val result = encodeAsText(
			"Binary Eye",
			BarcodeFormat.QR_CODE,
			inverted = true
		)
		assertEquals(
			"""█████████████████████████████
█████████████████████████████
████ ▄▄▄▄▄ █  ▄▄ █ ▄▄▄▄▄ ████
████ █   █ █ ▀▄ ██ █   █ ████
████ █▄▄▄█ █▄█▀▄██ █▄▄▄█ ████
████▄▄▄▄▄▄▄█ ▀▄█ █▄▄▄▄▄▄▄████
████▀▄▀▄█ ▄█ █ ▄█  █ ▄▄▄▄████
█████▀█▀ ▄▄ █▄▀▄ ▄▄██▀▀▄▀████
████▄███▄▄▄▄ ▄███ ▄    █ ████
████ ▄▄▄▄▄ █▀█ ██▀▀███  █████
████ █   █ █▀▀▄▄█ █▄▀▀█ █████
████ █▄▄▄█ █▀▄▄▄█▄▀█▄ █▀▀████
████▄▄▄▄▄▄▄██▄▄████▄▄██▄█████
█████████████████████████████
█████████████████████████████
""", result
		)
	}
}
