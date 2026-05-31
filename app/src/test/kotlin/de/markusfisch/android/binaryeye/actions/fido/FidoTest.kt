package de.markusfisch.android.binaryeye.actions.fido

import junit.framework.TestCase.assertEquals
import org.junit.Test

class FidoTest {
	@Test
	fun correct() {
		val urls = listOf(
			"fido:/1234567890",
			"FIDO:/1234567890",
		)
		for (url in urls) {
			assertEquals(url, resolve(url))
		}
	}

	@Test
	fun incorrect() {
		val urls = listOf(
			"",
			"fido:",
			"fido:/",
			"fido://1234567890",
			"fido:/abc",
			"fido:/123abc",
			"https://example.com",
		)
		for (url in urls) {
			assertEquals(null, resolve(url))
		}
	}
}

private fun resolve(s: String) = if (
	FidoAction.canExecuteOn(s.toByteArray())
) s else null
