package de.markusfisch.android.binaryeye.text

import junit.framework.TestCase.assertEquals
import org.junit.Test

class EscapeTest {
	@Test
	fun oneLetter() {
		assertEquals("foo\nbar", "foo\\nbar".unescape())
		assertEquals("foo\tbar", "foo\\tbar".unescape())
		assertEquals("\tfoo\tbar\n", "\\tfoo\\tbar\\n".unescape())
	}

	@Test
	fun specialChars() {
		assertEquals("foo\\bar", "foo\\\\bar".unescape())
		assertEquals("foo\"bar", "foo\\\"bar".unescape())
	}

	@Test
	fun hexCode() {
		assertEquals("Hex", "\\x48\\x65\\x78".unescape())
		assertEquals("☺", "\\u263A".unescape())
	}

	@Test
	fun incomplete() {
		assertEquals("foo\\", "foo\\".unescape())
	}
}
