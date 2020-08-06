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
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

fun Scan.sendAsync(context: Context, url: String, type: String) {
	CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
		val code = send(url, type)
		withContext(Dispatchers.Main) {
			if (code < 200 || code > 299) {
				context.toast(R.string.background_request_failed)
			}
		}
	}
}

private fun Scan.send(url: String, type: String): Int {
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
		"3" -> request(url) {  con ->
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
): Int {
	var con: HttpURLConnection? = null
	return try {
		con = URL(url).openConnection() as HttpURLConnection
		writer?.invoke(con)
		con.responseCode
	} catch (e: IOException) {
		-1
	} finally {
		con?.disconnect()
	}
}
