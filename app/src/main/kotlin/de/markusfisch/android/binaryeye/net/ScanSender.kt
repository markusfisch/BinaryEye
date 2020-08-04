package de.markusfisch.android.binaryeye.net

import android.content.Context
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.binaryeye.widget.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

fun Scan.send(context: Context, url: String) {
	CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
		val urlAndContent = url + URLEncoder.encode(content, "utf-8")
		val code = request(urlAndContent)
		withContext(Dispatchers.Main) {
			if (code < 200 || code > 299) {
				context.toast(R.string.background_request_failed)
			}
		}
	}
}

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
