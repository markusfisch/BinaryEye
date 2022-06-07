package de.markusfisch.android.binaryeye.actions.mail

import junit.framework.TestCase.assertEquals
import org.junit.Test

class MatMsgTest {
	@Test
	fun to() {
		val mm = MatMsg(
			"MATMSG:TO:someone@example.org;;"
		)
		assertEquals(mm.to, "someone@example.org")
	}

	@Test
	fun toSub() {
		val mm = MatMsg(
			"MATMSG:TO:someone@example.org;SUB:Stuff;;"
		)
		assertEquals(mm.to, "someone@example.org")
		assertEquals(mm.sub, "Stuff")
	}

	@Test
	fun toSubBody() {
		val mm = MatMsg(
			"MATMSG:TO:someone@example.org;SUB:Stuff;BODY:This is some text;;"
		)
		assertEquals(mm.to, "someone@example.org")
		assertEquals(mm.sub, "Stuff")
		assertEquals(mm.body, "This is some text")
	}

	@Test
	fun differentOrder() {
		val mm = MatMsg(
			"MATMSG:SUB:Stuff;BODY:This is some text;TO:someone@example.org;;"
		)
		assertEquals(mm.to, "someone@example.org")
		assertEquals(mm.sub, "Stuff")
		assertEquals(mm.body, "This is some text")
	}
}
