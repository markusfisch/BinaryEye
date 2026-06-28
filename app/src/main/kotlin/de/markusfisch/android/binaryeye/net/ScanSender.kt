package de.markusfisch.android.binaryeye.net

import android.content.Context
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.content.openUrl
import de.markusfisch.android.binaryeye.database.Scan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL

private const val SEND_TYPE_GET_ADD_CONTENT = "0"
private const val SEND_TYPE_GET_QUERY_STRING = "1"
private const val SEND_TYPE_POST_FORM = "2"
private const val SEND_TYPE_POST_JSON = "3"
private const val SEND_TYPE_EXTERNAL_BROWSER = "4"

fun Context.sendAsync(
	scan: Scan,
	url: String = prefs.sendScanUrl,
	type: String = prefs.sendScanType,
	callback: (Int?, String?) -> Unit
) {
	val extras = mapOf(
		"deviceId" to prefs.sendScanDeviceId.trim()
	)
	if (type == SEND_TYPE_EXTERNAL_BROWSER) {
		openUrl(buildUrl(url, scan.asUrlArguments(extras)))
		return
	}
	CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
		val response = scan.send(url, type, extras) ?: return@launch
		withContext(Dispatchers.Main) {
			callback(response.code, response.body)
		}
	}
}

private fun Scan.send(
	url: String,
	type: String,
	extras: Map<String, String>
): Response? = when (type) {
	SEND_TYPE_GET_ADD_CONTENT -> request(
		url + text.urlEncode()
	)

	SEND_TYPE_GET_QUERY_STRING -> request(
		buildUrl(url, asUrlArguments(extras))
	)

	SEND_TYPE_POST_FORM -> request(url) { con ->
		con.requestMethod = "POST"
		con.setRequestProperty(
			"Content-Type",
			"application/x-www-form-urlencoded"
		)
		con.outputStream.apply {
			write(asUrlArguments(extras).toByteArray())
			close()
		}
	}

	SEND_TYPE_POST_JSON -> request(url) { con ->
		con.requestMethod = "POST"
		con.setRequestProperty("Content-Type", "application/json")
		con.outputStream.apply {
			write(asJson(extras).toString().toByteArray())
			close()
		}
	}

	else -> null
}

private fun buildUrl(url: String, arguments: String): String {
	if (arguments.isEmpty()) {
		return url
	}
	val separator = when {
		url.endsWith("?") || url.endsWith("&") -> ""
		url.contains("?") -> "&"
		else -> "?"
	}
	return url + separator + arguments
}

private fun Scan.asUrlArguments(
	extras: Map<String, String>
): String = getMap(extras).map { (k, v) ->
	"${k}=${v.urlEncode()}"
}.joinToString("&")

private fun Scan.asJson(
	extras: Map<String, String>
) = JSONObject().apply {
	getMap(extras).forEach { (k, v) -> put(k, v) }
}

private fun Scan.getMap(extras: Map<String, String>): Map<String, String> = (mapOf(
	"content" to text,
	"raw" to raw?.toHexString(),
	"format" to format.name,
	"errorCorrectionLevel" to errorCorrectionLevel,
	"version" to version,
	"sequenceSize" to sequenceSize.toString(),
	"sequenceIndex" to sequenceIndex.toString(),
	"sequenceId" to sequenceId,
	"country" to country,
	"addOn" to addOn,
	"price" to price,
	"issueNumber" to issueNumber,
	"timestamp" to dateTime,
) + extras).filterNullValues()

private fun Map<String, String?>.filterNullValues() =
	filterValues { it != null }.mapValues { it.value as String }

private fun request(
	url: String,
	writer: ((HttpURLConnection) -> Any)? = null
): Response {
	var con: HttpURLConnection? = null
	return try {
		con = URL(url).openConnection() as HttpURLConnection
		con.connectTimeout = 5000
		writer?.invoke(con)
		val body = con.inputStream.readHead()
		Response(con.responseCode, body)
	} catch (e: ProtocolException) {
		Response(null, e.message)
	} catch (e: IOException) {
		try {
			val body = con?.errorStream?.readHead() ?: e.message
			Response(con?.responseCode, body)
		} catch (e: IOException) {
			Response(null, e.message)
		}
	} finally {
		con?.disconnect()
	}
}

private fun InputStream.readHead(): String {
	val sb = StringBuilder()
	val br = BufferedReader(InputStreamReader(this))
	var line = br.readLine()
	var i = 0
	while (line != null && sb.length < 240) {
		sb.append(line)
		line = br.readLine()
		++i
	}
	br.close()
	return sb.toString()
}

private data class Response(val code: Int?, val body: String?)
