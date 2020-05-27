package de.markusfisch.android.binaryeye.actions.vtype

import de.markusfisch.android.binaryeye.simpleFail
import junit.framework.TestCase.*
import org.junit.Test

class VTypeTest {
	@Test
	fun vcard() {
		val content = """BEGIN:VCARD
N:Doe;John
END:VCARD"""

		assertEquals("VCARD", VTypeParser.parseVType(content))

		val info = VTypeParser.parseMap(content)

		info["N"]?.singleOrNull()?.also {
			assertEquals("Doe;John", it.value)
		}
	}

	@Test
	fun vcalendar() {
		val content = """BEGIN:VCALENDAR
BEGIN:VEVENT
SUMMARY:foo
DTSTART:20080504T123456Z
DTEND:20080505T234555Z
END:VEVENT
END:VCALENDAR"""

		assertEquals("VCALENDAR", VTypeParser.parseVType(content))

		val info = VTypeParser.parseMap(content)

		info["SUMMARY"]?.singleOrNull()?.also {
			assertEquals("foo", it.value)
		}
		info["DTSTART"]?.singleOrNull()?.also {
			assertEquals("20080504T123456Z", it.value)
		}
		info["DTEND"]?.singleOrNull()?.also {
			assertEquals("20080505T234555Z", it.value)
		}
	}

	@Test
	fun vevent() {
		val content = """BEGIN:VEVENT
SUMMARY:foo
DTSTART:20080504T123456Z
DTEND:20080505T234555Z
END:VEVENT"""

		assertEquals("VEVENT", VTypeParser.parseVType(content))

		val info = VTypeParser.parseMap(content)

		info["SUMMARY"]?.singleOrNull()?.also {
			assertEquals("foo", it.value)
		}
		info["DTSTART"]?.singleOrNull()?.also {
			assertEquals("20080504T123456Z", it.value)
		}
		info["DTEND"]?.singleOrNull()?.also {
			assertEquals("20080505T234555Z", it.value)
		}
	}

	@Test
	fun veventWithCarriageReturn() {
		val content = """BEGIN:VEVENT${'\r'}
SUMMARY:foo${'\r'}
DTSTART:20080504T123456Z${'\r'}
DTEND:20080505T234555Z${'\r'}
END:VEVENT"""

		assertEquals("VEVENT", VTypeParser.parseVType(content))

		val info = VTypeParser.parseMap(content)

		info["SUMMARY"]?.singleOrNull()?.also {
			assertEquals("foo", it.value)
		}
		info["DTSTART"]?.singleOrNull()?.also {
			assertEquals("20080504T123456Z", it.value)
		}
		info["DTEND"]?.singleOrNull()?.also {
			assertEquals("20080505T234555Z", it.value)
		}
	}
}
