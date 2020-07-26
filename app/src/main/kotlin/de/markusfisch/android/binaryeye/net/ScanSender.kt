package de.markusfisch.android.binaryeye.net

import de.markusfisch.android.binaryeye.database.Scan
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

fun Scan.send(url: String) {
	thread {
		var con: HttpURLConnection? = null
		try {
			con = URL(
				url + URLEncoder.encode(content, "utf-8")
			).openConnection() as HttpURLConnection
			con.responseCode
		} catch (e: IOException) {
			// can't do anything about it
		} finally {
			con?.disconnect()
		}
	}
}
