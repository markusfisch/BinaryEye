package de.markusfisch.android.binaryeye.net

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BinaryEyeDeeplinkTest {

    @Test
    fun isScanDeeplinkTests() {
        // Test for binaryeye://scan
        assertEquals(true, isScanDeeplink("binaryeye://scan"))
        assertEquals(true, isScanDeeplink("binaryeye://scan?param=value"))
        
        // Test for http://markusfisch.de/BinaryEye
        assertEquals(true, isScanDeeplink("http://markusfisch.de/BinaryEye"))
        assertEquals(true, isScanDeeplink("http://markusfisch.de/BinaryEye?param=value"))
        
        // Test for https://markusfisch.de/BinaryEye
        assertEquals(true, isScanDeeplink("https://markusfisch.de/BinaryEye"))
        assertEquals(true, isScanDeeplink("https://markusfisch.de/BinaryEye?param=value"))
        
        // Negative tests
        assertEquals(false, isScanDeeplink("binaryeye://encode"))
        assertEquals(false, isScanDeeplink("http://markusfisch.de/encode"))
        assertEquals(false, isScanDeeplink("https://markusfisch.de/encode"))
        assertEquals(false, isScanDeeplink("http://example.com"))
        assertEquals(false, isScanDeeplink(""))
    }

    @Test
    fun isContinuousScanDeeplinkTests() {
        assertEquals(true, isContinuousScanDeeplink("binaryeye://scan?continuous=true"))
        assertEquals(true, isContinuousScanDeeplink("https://markusfisch.de/BinaryEye?continuous=true"))
        assertEquals(false, isContinuousScanDeeplink("binaryeye://scan"))
        assertEquals(false, isContinuousScanDeeplink("binaryeye://scan?continuous=false"))
        assertEquals(false, isContinuousScanDeeplink("binaryeye://scan?continuous=1"))
        assertEquals(false, isContinuousScanDeeplink("binaryeye://encode?continuous=true"))
    }

    @Test
    fun acceptsOnlyHttpCallbackEndpoints() {
        assertEquals(true, isHttpCallback("https://example.com/scan?session=123"))
        assertEquals(true, isHttpCallback("http://192.168.1.2:8080/scan"))
        assertEquals(false, isHttpCallback("javascript:alert(1)"))
        assertEquals(false, isHttpCallback("binaryeye://scan"))
        assertEquals(false, isHttpCallback("https:///scan"))
        assertEquals(false, isHttpCallback("https://user:password@example.com/scan"))
        assertEquals(false, isHttpCallback("https://example.com/scan#fragment"))
        assertEquals(false, isHttpCallback("https://example.com:99999/scan"))
        assertEquals(false, isHttpCallback(""))
    }

    @Test
    fun isEncodeDeeplinkTests() {
        // Test for binaryeye://encode
        assertEquals(true, isEncodeDeeplink("binaryeye://encode"))
        assertEquals(true, isEncodeDeeplink("binaryeye://encode?param=value"))
        
        // Test for http://markusfisch.de/encode
        assertEquals(true, isEncodeDeeplink("http://markusfisch.de/encode"))
        assertEquals(true, isEncodeDeeplink("http://markusfisch.de/encode?param=value"))
        
        // Test for https://markusfisch.de/encode
        assertEquals(true, isEncodeDeeplink("https://markusfisch.de/encode"))
        assertEquals(true, isEncodeDeeplink("https://markusfisch.de/encode?param=value"))
        
        // Negative tests
        assertEquals(false, isEncodeDeeplink("binaryeye://scan"))
        assertEquals(false, isEncodeDeeplink("http://markusfisch.de/BinaryEye"))
        assertEquals(false, isEncodeDeeplink("https://markusfisch.de/BinaryEye"))
        assertEquals(false, isEncodeDeeplink("http://example.com"))
        assertEquals(false, isEncodeDeeplink(""))
    }

    @Test
    fun createEncodeDeeplinkTests() {
        // Basic test
        assertEquals(
            "https://markusfisch.de/encode?format=QRCode&content=test",
            createEncodeDeeplink("QRCode", "test")
        )
        
        // Test with special characters in content
        assertEquals(
            "https://markusfisch.de/encode?format=QRCode&content=test%20with%20spaces",
            createEncodeDeeplink("QRCode", "test with spaces")
        )
        
        // Test with different barcode format
        assertEquals(
            "https://markusfisch.de/encode?format=Code128&content=123456",
            createEncodeDeeplink("Code128", "123456")
        )
    }
}
