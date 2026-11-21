package de.markusfisch.android.binaryeye.automation

import android.content.Intent
import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutomatedActionTest {
	@Test
	fun preservesScanTextInIntentExtra() {
		val text = "a;b=c + % # ü"
		val scan = Scan(text, null, BarcodeFormat.QRCode)
		val template = "intent:#Intent;action=android.intent.action.SEND;" +
			"type=text/plain;S.android.intent.extra.TEXT={RESULT};end"
		val intent = Intent.parseUri(template.buildIntentUri(scan), Intent.URI_INTENT_SCHEME)

		assertEquals(Intent.ACTION_SEND, intent.action)
		assertEquals("text/plain", intent.type)
		assertEquals(text, intent.getStringExtra(Intent.EXTRA_TEXT))
	}
}
