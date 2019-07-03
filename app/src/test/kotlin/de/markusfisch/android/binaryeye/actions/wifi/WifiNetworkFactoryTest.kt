package de.markusfisch.android.binaryeye.actions.wifi

import junit.framework.TestCase.*
import org.junit.Test

class WifiNetworkFactoryTest {
    @Test
    fun notWifi() {
        assertNull(WifiNetworkFactory.parse("asdf"))
    }

    @Test
    fun wep() {
        val info = WifiNetworkFactory.parse("WIFI:T:WEP;S:asdf;P:password;;")
        assertNotNull(info)

        info?.let {
            assertEquals("WEP", it.security)
            assertEquals("asdf", it.ssid)
            assertEquals("password", it.password)
            assertFalse(it.hidden)
        }
    }

    @Test
    fun hidden() {
        val info = WifiNetworkFactory.parse("WIFI:T:WEP;S:asdf;P:password;H;")
        info?.let {
            assertEquals("WEP", it.security)
            assertEquals("asdf", it.ssid)
            assertEquals("password", it.password)
            assertTrue(it.hidden)
        }
    }

    @Test
    fun nopass() {
        val info = WifiNetworkFactory.parse( "WIFI:T:nopass;S:asdf;;;")
        info?.let {
            assertEquals(null, it.security)
            assertEquals("asdf", it.ssid)
            assertNull(it.password)
        }
    }

    @Test
    fun plainUnsecured() {
        val info = WifiNetworkFactory.parse("WIFI:;S:asdf;;;")
        info?.let {
            assertEquals(null, it.security)
            assertEquals("asdf", it.ssid)
        }
    }
}