package de.markusfisch.android.binaryeye.actions.vtype

import junit.framework.TestCase.assertEquals
import org.junit.Test

class VTypeTest {
	@Test
	fun vcard() {
		val content = """BEGIN:VCARD
N:Doe;John
TEL:511
END:VCARD"""

		assertEquals("VCARD", VTypeParser.parseVType(content))

		val info = VTypeParser.parseMap(content)

		assertEquals("Doe;John", info["N"]?.singleOrNull()?.value)
		assertEquals("511", info["TEL"]?.singleOrNull()?.value)
	}

	@Test
	fun vcardWithExplicitTelType() {
		val content = """BEGIN:VCARD
N:Doe;John
TEL;TYPE=WORK:511
TEL;HOME:311
END:VCARD"""

		assertEquals("VCARD", VTypeParser.parseVType(content))

		val info = VTypeParser.parseMap(content)

		assertEquals("Doe;John", info["N"]?.singleOrNull()?.value)
		val work = info["TEL"]?.get(0)
		assertEquals("511", work?.value)
		assertEquals("WORK", work?.firstTypeOrFirstInfo)
		val home = info["TEL"]?.get(1)
		assertEquals("311", home?.value)
		assertEquals("HOME", home?.firstTypeOrFirstInfo)
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

		assertEquals(
			"foo",
			info["SUMMARY"]?.singleOrNull()?.value
		)
		assertEquals(
			"20080504T123456Z",
			info["DTSTART"]?.singleOrNull()?.value
		)
		assertEquals(
			"20080505T234555Z",
			info["DTEND"]?.singleOrNull()?.value
		)
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

		assertEquals(
			"foo",
			info["SUMMARY"]?.singleOrNull()?.value
		)
		assertEquals(
			"20080504T123456Z",
			info["DTSTART"]?.singleOrNull()?.value
		)
		assertEquals(
			"20080505T234555Z",
			info["DTEND"]?.singleOrNull()?.value
		)
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

		assertEquals(
			"foo",
			info["SUMMARY"]?.singleOrNull()?.value
		)
		assertEquals(
			"20080504T123456Z",
			info["DTSTART"]?.singleOrNull()?.value
		)
		assertEquals(
			"20080505T234555Z",
			info["DTEND"]?.singleOrNull()?.value
		)
	}
}
