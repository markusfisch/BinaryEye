package de.markusfisch.android.binaryeye.preference

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PreferencesTest {
	@Test
	fun forwardingDoesNotCarryOverBetweenProfiles() {
		val context = RuntimeEnvironment.getApplication()
		val prefs = Preferences().apply { init(context) }
		prefs.load(context, "forwarding")
		prefs.sendScanActive = true
		prefs.sendScanUrl = "https://example.com"
		prefs.sendScanType = "3"
		prefs.sendScanDeviceId = "office"
		prefs.sendScanBluetooth = true
		prefs.sendScanBluetoothHost = "receiver"
		prefs.sendScanKdeConnect = true

		prefs.load(context, "empty")

		assertFalse(prefs.sendScanActive)
		assertEquals("", prefs.sendScanUrl)
		assertEquals("0", prefs.sendScanType)
		assertEquals("", prefs.sendScanDeviceId)
		assertFalse(prefs.sendScanBluetooth)
		assertEquals("", prefs.sendScanBluetoothHost)
		assertFalse(prefs.sendScanKdeConnect)

		prefs.load(context, "forwarding")
		assertEquals("https://example.com", prefs.sendScanUrl)
	}
}
