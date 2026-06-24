package de.markusfisch.android.binaryeye.kdeconnect

import android.content.Intent
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KdeConnectSenderTest {
	@Test
	fun createKdeConnectIntent() {
		val intent = createKdeConnectIntent("test scan")

		assertEquals(Intent.ACTION_SEND, intent.action)
		assertEquals("text/x-keystrokes", intent.type)
		assertEquals("org.kde.kdeconnect_tp", intent.component?.packageName)
		assertEquals(
			"org.kde.kdeconnect.plugins.mousepad.SendKeystrokesToHostActivity",
			intent.component?.className
		)
		assertEquals("test scan", intent.getStringExtra(Intent.EXTRA_TEXT))
	}
}
