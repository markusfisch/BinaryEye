package de.markusfisch.android.binaryeye.actions.wifi

import de.markusfisch.android.binaryeye.simpleFail
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class WifiConnectorTest {
	@Test
	fun notWifi() {
		assertNull(WifiConnector.parseMap("asdfz"))
	}

	@Test
	fun wep() {
		val info = simpleDataAccessor("WIFI:T:WEP;S:asdfz;P:password;;")

		assertEquals("WEP", info.securityType)
		assertEquals("asdfz", info.ssid)
		assertEquals("password", info.password)
		assertFalse(info.hidden)
	}

	@Test
	fun anonymousIdentity() {
		val info = simpleDataAccessor("WIFI:T:WPA2-EAP;S:wifi;P:password;I:ident;A:anonymous;;")

		assertEquals("WPA2-EAP", info.securityType)
		assertEquals("wifi", info.ssid)
		assertEquals("password", info.password)
		assertEquals("ident", info.identity)
		assertEquals("anonymous", info.anonymousIdentity)
		assertFalse(info.hidden)
	}

	@Test
	fun anonymousIdentityAI() {
		val info = simpleDataAccessor("WIFI:T:WPA2-EAP;S:wifi;P:password;I:ident;AI:anonymous;;")

		assertEquals("WPA2-EAP", info.securityType)
		assertEquals("wifi", info.ssid)
		assertEquals("password", info.password)
		assertEquals("ident", info.identity)
		assertEquals("anonymous", info.anonymousIdentity)
		assertFalse(info.hidden)
	}

	@Test
	fun hidden() {
		val info = simpleDataAccessor("WIFI:T:WPA;S:asdfz;P:password;H:true;;")

		assertEquals("WPA", info.securityType)
		assertEquals("asdfz", info.ssid)
		assertEquals("password", info.password)
		assertTrue(info.hidden)
	}

	@Test
	fun nopass() {
		val info = simpleDataAccessor("WIFI:T:nopass;S:asdfz;;")

		assertEquals("nopass", info.securityType)
		assertEquals("asdfz", info.ssid)
		assertNull(info.password)
		assertFalse(info.hidden)
	}

	@Test
	fun plainUnsecured() {
		val info = simpleDataAccessor("WIFI:S:asdfz;;")

		assertEquals("", info.securityType)
		assertEquals("asdfz", info.ssid)
		assertNull(info.password)
		assertFalse(info.hidden)
	}

	@Test
	fun escaping() {
		val info = simpleDataAccessor("""WIFI:S:\"ssid\\\;stillSSID\:\;x;;""")

		assertEquals("", info.securityType)
		assertEquals("\"ssid\\;stillSSID:;x", info.ssid)
		assertNull(info.password)
		assertFalse(info.hidden)
	}

	@Test
	fun wrongEscaping() {
		val info = simpleDataAccessor("""WIFI:S:\SSID":\;x;""")

		assertEquals("", info.securityType)
		assertEquals("\\SSID\":;x", info.ssid)
		assertNull(info.password)
		assertFalse(info.hidden)
	}

	private fun simpleDataAccessor(wifiString: String): WifiConnector.SimpleDataAccessor {
		val map = WifiConnector.parseMap(wifiString)
			?: simpleFail("parsing map of valid string fails ($wifiString)")
		return WifiConnector.SimpleDataAccessor.of(map)
			?: simpleFail("could not create SimpleDataAccessor of (potentially) valid map ($map of $wifiString)")
	}
}
