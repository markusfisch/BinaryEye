package de.markusfisch.android.binaryeye

import de.markusfisch.android.binaryeye.actions.WifiAction
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class WifiActionTest {
    @Test
    fun notWifi() {
        val data = "asdf".toByteArray()
        assertFalse(WifiAction.canExecuteOn(data))
    }

    @Test
    fun wep() {
        val data = "WIFI:T:WEP;S:asdf;P:password;;".toByteArray()
        assertTrue(WifiAction.canExecuteOn(data))
    }

    @Test
    fun hidden() {
        val data = "WIFI:T:WEP;S:asdf;P:password;H;".toByteArray()
        assertTrue(WifiAction.canExecuteOn(data))
    }
}