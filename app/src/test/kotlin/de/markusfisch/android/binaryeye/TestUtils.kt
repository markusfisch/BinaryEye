package de.markusfisch.android.binaryeye

import junit.framework.TestCase.fail

fun simpleFail(message: String = "Unknown reason, but failed"): Nothing {
	fail(message)
	throw IllegalStateException("You should never have reached this point in the code, but anyways: $message")
}

inline fun <reified E : Throwable> assertThrows(
	message: String = "Unknown reason, but didn't failed even though should fail",
	block: () -> Unit
) {
	try {
		block()
		fail(message)
	} catch (e: Throwable) {
		if (e !is E) {
			fail("Got unexpected other Throwable: ${e::class.java.simpleName}\n$e")
		}
	}
}
