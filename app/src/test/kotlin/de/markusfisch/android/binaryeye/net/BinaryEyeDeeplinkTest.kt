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
            "https://markusfisch.de/encode?format=QR_CODE&content=test",
            createEncodeDeeplink("QR_CODE", "test")
        )
        
        // Test with special characters in content
        assertEquals(
            "https://markusfisch.de/encode?format=QR_CODE&content=test%20with%20spaces",
            createEncodeDeeplink("QR_CODE", "test with spaces")
        )
        
        // Test with different barcode format
        assertEquals(
            "https://markusfisch.de/encode?format=CODE_128&content=123456",
            createEncodeDeeplink("CODE_128", "123456")
        )
    }
}