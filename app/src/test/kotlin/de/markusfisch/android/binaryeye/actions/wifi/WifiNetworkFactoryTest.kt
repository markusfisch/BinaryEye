package de.markusfisch.android.binaryeye.actions.wifi

import junit.framework.TestCase.*
import org.junit.Test

class WifiNetworkFactoryTest {
    @Test
    fun notWifi() {
        assertNull(WifiNetworkFactory.parse("asdfz"))
    }

    @Test
    fun wep() {
        val info = WifiNetworkFactory.parse("WIFI:T:WEP;S:asdfz;P:password;;")
        assertNotNull(info)

        info?.let {
            assertEquals("WEP", it.security)
            assertEquals("\"asdfz\"", it.ssid)
            assertEquals("\"password\"", it.password)
            assertFalse(it.hidden)
        }
    }

    @Test
    fun hidden() {
        val info = WifiNetworkFactory.parse("WIFI:T:WEP;S:asdfz;P:password;H;")
        info?.let {
            assertEquals("WEP", it.security)
            assertEquals("\"asdfz\"", it.ssid)
            assertEquals("\"password\"", it.password)
            assertTrue(it.hidden)
        }
    }

    @Test
    fun nopass() {
        val info = WifiNetworkFactory.parse( "WIFI:T:nopass;S:asdfz;;;")
        info?.let {
            assertEquals(null, it.security)
            assertEquals("\"asdfz\"", it.ssid)
            assertNull(it.password)
        }
    }

    @Test
    fun plainUnsecured() {
        val info = WifiNetworkFactory.parse("WIFI:;S:asdfz;;;")
        info?.let {
            assertEquals(null, it.security)
            assertEquals("\"asdfz\"", it.ssid)
        }
    }

    @Test
    fun hex() {
        val info = WifiNetworkFactory.parse("WIFI:T:WEP;S:d34dbeef;P:d34dbeef;;")
        info?.let {
            assertEquals("d34dbeef", it.ssid)
            assertEquals("d34dbeef", it.password)
        }
    }
}