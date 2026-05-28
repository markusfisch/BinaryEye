package de.markusfisch.android.binaryeye.net

import junit.framework.TestCase.assertEquals
import org.junit.Test

class ScanSenderTest {
	@Test
	fun appendUrlArgumentsTests() {
		assertEquals(
			"https://example.com",
			appendUrlArguments("https://example.com", "")
		)
		assertEquals(
			"https://example.com?deviceId=1",
			appendUrlArguments("https://example.com", "deviceId=1")
		)
		assertEquals(
			"https://example.com?content=test&deviceId=1",
			appendUrlArguments(
				"https://example.com?content=test",
				"deviceId=1"
			)
		)
		assertEquals(
			"https://example.com?deviceId=1",
			appendUrlArguments("https://example.com?", "deviceId=1")
		)
		assertEquals(
			"https://example.com?content=test&deviceId=1",
			appendUrlArguments(
				"https://example.com?content=test&",
				"deviceId=1"
			)
		)
	}
}
