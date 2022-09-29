package de.markusfisch.android.binaryeye.actions.web

import junit.framework.TestCase.assertEquals
import org.junit.Test

class WebTest {
	@Test
	fun correct() {
		val urls = listOf(
			"https://google.com",
			"https://markusfisch.de",
			"http://markusfisch.de",
			"http://www.markusfisch.de",
			"http://www.markusfisch.de/",
			"http://www.markusfisch.de/?",
			"http://www.markusfisch.de/?foo=bar",
			"https://markusfisch.de/foo",
			"https://markusfisch.de/foo#bar",
			"https://foo.example.com/",
			"http://example.com/",
		)
		for (url in urls) {
			assertEquals(url, resolve(url))
		}
	}

	@Test
	fun colloquial() {
		val urls = listOf(
			"google.com",
			"GOOGLE.com",
			"GOOGLE.COM",
			" foo.com",
			"foo.com ",
			" foo.com ",
			"  foo.com ",
			"foo.com",
			"foo.com/bar",
			"foo.com?bar",
			"foo.com#bar",
		)
		for (url in urls) {
			assertEquals(url, resolve(url))
		}
	}


	@Test
	fun noUrls() {
		val urls = listOf(
			"",
			"foo",
			"foo . bar",
			"exa mple.com",
			"example.co m",
			"some string",
			"some string.",
			".foo",
			"..foo",
			"...foo",
			"foo.",
			"foo..",
			"foo...",
			"foo. foo",
		)
		for (url in urls) {
			assertEquals(null, resolve(url))
		}
	}
}

private fun resolve(s: String) = if (
	WebAction.canExecuteOn(s.toByteArray())
) s else null
