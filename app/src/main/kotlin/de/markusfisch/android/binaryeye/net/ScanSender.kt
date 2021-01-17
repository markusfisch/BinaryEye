package de.markusfisch.android.binaryeye.net

import android.content.Context
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.toHexString
import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.binaryeye.widget.toast
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
import java.net.URLEncoder

fun Scan.sendAsync(context: Context, url: String, type: String) {
	CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
		val response = send(url, type)
		withContext(Dispatchers.Main) {
			if (response.body != null && response.body.isNotEmpty()) {
				context.toast(response.body)
			} else if (response.code == null || response.code > 299) {
				context.toast(R.string.background_request_failed)
			}
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
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
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
			url + urlEncode(content)
		)
	}
}

private fun Scan.asUrlArguments(): String = getMap().map { (k, v) ->
	"${k}=${urlEncode(v)}"
}.joinToString("&")

private fun Scan.asJson() = JSONObject().apply {
	getMap().forEach { (k, v) -> put(k, v) }
}

private fun urlEncode(s: String) = URLEncoder.encode(s, "utf-8")

private fun Scan.getMap(): Map<String, String> = mapOf(
	"content" to content,
	"raw" to raw?.toHexString(),
	"format" to format,
	"errorCorrectionLevel" to errorCorrectionLevel,
	"issueNumber" to issueNumber,
	"orientation" to orientation,
	"otherMetaData" to otherMetaData,
	"pdf417ExtraMetaData" to pdf417ExtraMetaData,
	"possibleCountry" to possibleCountry,
	"suggestedPrice" to suggestedPrice,
	"upcEanExtension" to upcEanExtension,
	"timestamp" to timestamp
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
		writer?.invoke(con)
		val body = con.inputStream.readHead()
		Response(con.responseCode, body)
	} catch (e: ProtocolException) {
		Response(null, e.message)
	} catch (e: IOException) {
		val body = con?.errorStream?.readHead()
		Response(con?.responseCode, body)
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