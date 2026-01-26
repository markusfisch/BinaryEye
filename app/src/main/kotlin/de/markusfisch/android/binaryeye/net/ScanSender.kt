package de.markusfisch.android.binaryeye.net

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

fun Scan.sendAsync(
	url: String,
	type: String,
	callback: (Int?, String?) -> Unit
) {
	CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
		val response = send(url, type)
		withContext(Dispatchers.Main) {
			callback(response.code, response.body)
		}
	}
}

private fun Scan.send(url: String, type: String): Response {
	return when (type) {
		"1" -> request(
			url + "?" + asUrlArguments()
		)

		"2" -> request(url) { con ->
			con.requestMethod = "POST"
			con.setRequestProperty(
				"Content-Type",
				"application/x-www-form-urlencoded"
			)
			con.outputStream.apply {
				write(asUrlArguments().toByteArray())
				close()
			}
		}

		"3" -> request(url) { con ->
			con.requestMethod = "POST"
			con.setRequestProperty("Content-Type", "application/json")
			con.outputStream.apply {
				write(asJson().toString().toByteArray())
				close()
			}
		}

		else -> request(
			url + text.urlEncode()
		)
	}
}

private fun Scan.asUrlArguments(): String = getMap().map { (k, v) ->
	"${k}=${v.urlEncode()}"
}.joinToString("&")

private fun Scan.asJson() = JSONObject().apply {
	getMap().forEach { (k, v) -> put(k, v) }
}

private fun Scan.getMap(): Map<String, String> = mapOf(
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
	"timestamp" to dateTime
).filterNullValues()

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
