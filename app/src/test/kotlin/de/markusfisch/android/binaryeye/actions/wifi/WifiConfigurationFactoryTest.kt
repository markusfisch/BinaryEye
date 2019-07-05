package de.markusfisch.android.binaryeye.actions.wifi

import junit.framework.TestCase.*
import org.junit.Test
import java.lang.Exception

class WifiConfigurationFactoryTest {
	@Test
	fun notWifi() {
		try {
			WifiConfigurationFactory("asdfz").ssid
			fail("could access SSID in non wifi string, shouldn't be possible")
		} catch (ignore: Exception) {}
	}

	@Test
	fun wep() {
		val info = WifiConfigurationFactory("WIFI:T:WEP;S:asdfz;P:password;;")
		assertNotNull(info)

		assertEquals("WEP", info.securityType)
		assertEquals("\"asdfz\"", info.ssid)
		assertEquals("\"password\"", info.password)
		assertFalse(info.hidden)
	}

	@Test
	fun hidden() {
		val info = WifiConfigurationFactory("WIFI:T:WPA;S:asdfz;P:password;H:true;;")
		assertNotNull(info)

		assertEquals("WPA", info.securityType)
		assertEquals("\"asdfz\"", info.ssid)
		assertEquals("\"password\"", info.password)
		assertTrue(info.hidden)

	}

	@Test
	fun nopass() {
		val info = WifiConfigurationFactory( "WIFI:T:nopass;S:asdfz;;")
		assertNotNull(info)

		assertEquals("nopass", info.securityType)
		assertEquals("\"asdfz\"", info.ssid)
		assertNull(info.password)
	}

	@Test
	fun plainUnsecured() {
		val info = WifiConfigurationFactory("WIFI:S:asdfz;;")
		assertNotNull(info)

		assertEquals("", info.securityType)
		assertEquals("\"asdfz\"", info.ssid)
	}

	@Test
	fun hex() {
		val info = WifiConfigurationFactory("WIFI:T:WEP;S:d34dbeef;P:d34dbeef;;")
		assertNotNull(info)

		assertEquals("d34dbeef", info.ssid)
		assertEquals("d34dbeef", info.password)
	}

	@Test
	fun escaping() {
		val info = WifiConfigurationFactory("""WIFI:S:\"ssid\\\;stillSSID\:\;x;;""")
		assertNotNull(info)

		assertEquals("""""ssid\;stillSSID:;x"""", info.ssid)
	}

	@Test
	fun wrongEscaping() {
		val info = WifiConfigurationFactory("""WIFI:S:\SSID":\;x;""")
		assertNotNull(info)

		assertEquals(""""\SSID":;x"""", info.ssid)
	}
}
