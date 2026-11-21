package de.markusfisch.android.binaryeye.preference

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PreferencesTest {
	@Test
	fun profilePreviewDoesNotSaveSettings() {
		val context = RuntimeEnvironment.getApplication()
		val prefs = Preferences().apply { init(context) }
		val settings = context.getSharedPreferences("imported", 0)
		val profile = checkNotNull(prefs.parseProfileImport(
			"""{"type":"binaryeye_profile","name":"imported","use_history":true}"""
		))

		assertFalse("imported" in prefs.profiles)
		assertFalse(settings.contains("use_history"))

		assertTrue(prefs.importProfile(context, profile))
		assertTrue("imported" in prefs.profiles)
		assertTrue(settings.getBoolean("use_history", false))
	}

	@Test
	fun historyDoesNotCarryOverBetweenProfiles() {
		val context = RuntimeEnvironment.getApplication()
		val prefs = Preferences().apply { init(context) }
		prefs.load(context, "with_history")
		prefs.useHistory = true

		prefs.load(context, "empty")
		assertFalse(prefs.useHistory)

		prefs.load(context, "with_history")
		assertTrue(prefs.useHistory)
	}

	@Test
	fun invalidBulkModeDelayUsesDefault() {
		val context = RuntimeEnvironment.getApplication()
		val prefs = Preferences().apply { init(context) }
		val settings = context.getSharedPreferences("invalid_delay", 0)
		settings.edit().putString("bulk_mode_delay", "-1").apply()

		prefs.load(context, "invalid_delay")
		assertEquals(500, prefs.bulkModeDelay)

		val profile = checkNotNull(prefs.parseProfileImport(
			"""{"type":"binaryeye_profile","name":"delay","bulk_mode_delay":"invalid"}"""
		))
		assertTrue(prefs.importProfile(context, profile))
		val importedSettings = context.getSharedPreferences(profile.name, 0)
		assertEquals("500", importedSettings.getString("bulk_mode_delay", null))
	}

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
