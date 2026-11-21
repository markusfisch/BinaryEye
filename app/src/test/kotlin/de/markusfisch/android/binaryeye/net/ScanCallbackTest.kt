package de.markusfisch.android.binaryeye.net

import android.os.Looper
import com.sun.net.httpserver.HttpServer
import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
class ScanCallbackTest {
	private lateinit var server: HttpServer
	private lateinit var url: String
	private val bodies = CopyOnWriteArrayList<String>()
	private val methods = CopyOnWriteArrayList<String>()
	private val contentTypes = CopyOnWriteArrayList<String>()
	@Volatile
	private var responseCode = 204

	@Before
	fun startServer() {
		server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
		url = "http://127.0.0.1:${server.address.port}/scan"
		server.createContext("/scan") { exchange ->
			methods.add(exchange.requestMethod)
			contentTypes.add(exchange.requestHeaders.getFirst("Content-Type"))
			bodies.add(exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() })
			exchange.responseHeaders.add("Location", "$url/redirected")
			exchange.sendResponseHeaders(responseCode, -1)
			exchange.close()
		}
		server.start()
	}

	@After
	fun stopServer() {
		server.stop(0)
	}

	@Test
	fun postsUtf8JsonAndAcceptsEmptySuccessResponse() {
		val payload = JSONObject().put("content", "ä & \"\n你好").toString()
		assertEquals(204, postScanCallback(url, payload).code)
		assertEquals(listOf("POST"), methods)
		assertEquals(listOf("application/json; charset=utf-8"), contentTypes)
		assertEquals(listOf(payload), bodies)
	}

	@Test
	fun ignoresSuccessfulResponseBody() {
		server.createContext("/body") { exchange ->
			exchange.requestBody.use { it.readBytes() }
			val body = "unused".toByteArray()
			exchange.sendResponseHeaders(200, body.size.toLong())
			exchange.responseBody.use { it.write(body) }
		}
		assertEquals(Response(200, null), postScanCallback(url.replace("/scan", "/body"), "{}"))
	}

	@Test
	fun truncatesLongErrorLine() {
		server.createContext("/error") { exchange ->
			exchange.requestBody.use { it.readBytes() }
			val body = "x".repeat(1000).toByteArray()
			exchange.sendResponseHeaders(400, body.size.toLong())
			exchange.responseBody.use { it.write(body) }
		}
		val response = postScanCallback(url.replace("/scan", "/error"), "{}")
		assertEquals(Response(400, "x".repeat(240)), response)
	}

	@Test
	fun doesNotFollowRedirects() {
		responseCode = 302
		assertEquals(302, postScanCallback(url, "{}").code)
		assertEquals(1, bodies.size)
	}

	@Test
	fun reportsHttpErrorsAndConnectionFailures() {
		responseCode = 400
		assertEquals(400, postScanCallback(url, "{}").code)
		server.stop(0)
		assertNull(postScanCallback(url, "{}").code)
	}

	@Test
	fun timesOutWhenEndpointDoesNotRespond() {
		server.createContext("/slow") { exchange ->
			Thread.sleep(500)
			exchange.close()
		}
		assertNull(postScanCallback(url.replace("/scan", "/slow"), "{}", 100).code)
	}

	@Test
	fun pausesOnFailureAndRetriesIdenticalPayloadAfterRestoration() {
		val session = ContinuousScanSession()
		session.configure(url)
		assertTrue(session.blocked)
		assertEquals(ContinuousScanSession.State.APPROVAL, session.state.value)
		session.approve()
		assertFalse(session.blocked)
		responseCode = 503
		session.submit(Scan("ä\n你好", byteArrayOf(0, -1), BarcodeFormat.QRCode))
		assertTrue(session.blocked)
		session.retry()
		session.submit(Scan("another scan", null, BarcodeFormat.QRCode))
		awaitState(session, ContinuousScanSession.State.FAILED)
		val first = JSONObject(bodies.single())
		assertEquals("ä\n你好", first.getString("content"))
		assertEquals("00ff", first.getString("raw").lowercase())
		assertEquals("QRCode", first.getString("format"))
		assertTrue(first.getString("scanId").isNotEmpty())

		val restored = ContinuousScanSession()
		restored.configure(url, session.saveState())
		assertEquals(ContinuousScanSession.State.FAILED, restored.state.value)
		assertTrue(restored.blocked)
		assertEquals("ä\n你好", restored.lastText)
		responseCode = 204
		restored.retry()
		awaitState(restored, ContinuousScanSession.State.READY)
		assertFalse(restored.blocked)
		assertEquals(2, bodies.size)
		assertEquals(bodies[0], bodies[1])
		val completed = ContinuousScanSession()
		completed.configure(url, restored.saveState())
		assertEquals(ContinuousScanSession.State.READY, completed.state.value)
	}

	@Test
	fun distinguishesBinaryScansAfterRestoration() {
		val session = ContinuousScanSession()
		session.configure(url)
		session.approve()
		responseCode = 204
		session.submit(Scan("", byteArrayOf(0, 1), BarcodeFormat.QRCode))
		awaitState(session, ContinuousScanSession.State.READY)

		val restored = ContinuousScanSession()
		restored.configure(url, session.saveState())
		val lastScan = requireNotNull(restored.lastScan)
		assertTrue(lastScan.matches("", "QRCode", byteArrayOf(0, 1)))
		assertFalse(lastScan.matches("", "QRCode", byteArrayOf(0, 2)))
		assertFalse(lastScan.matches("", "DataMatrix", byteArrayOf(0, 1)))
	}

	private fun awaitState(
		session: ContinuousScanSession,
		state: ContinuousScanSession.State
	) {
		val deadline = System.nanoTime() + 5_000_000_000L
		while (session.state.value != state && System.nanoTime() < deadline) {
			shadowOf(Looper.getMainLooper()).idle()
			Thread.sleep(10)
		}
		assertEquals(state, session.state.value)
	}
}
