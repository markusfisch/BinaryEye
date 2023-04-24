package de.markusfisch.android.binaryeye.text

import junit.framework.TestCase.assertEquals
import org.junit.Test

class EscapeTest {
	@Test
	fun oneLetter() {
		assertEquals("foo\\nbar".unescape(), "foo\nbar")
		assertEquals("foo\\tbar".unescape(), "foo\tbar")
		assertEquals("\\tfoo\\tbar\\n".unescape(), "\tfoo\tbar\n")
	}

	@Test
	fun specialChars() {
		assertEquals("foo\\\\bar".unescape(), "foo\\bar")
		assertEquals("foo\\\"bar".unescape(), "foo\"bar")
	}

	@Test
	fun hexCode() {
		assertEquals("\\x48\\x65\\x78".unescape(), "Hex")
		assertEquals("\\u263A".unescape(), "☺")
	}

	@Test
	fun incomplete() {
		assertEquals("foo\\".unescape(), "foo\\")
	}
}
