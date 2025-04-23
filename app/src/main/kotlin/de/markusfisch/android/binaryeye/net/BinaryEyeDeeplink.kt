package de.markusfisch.android.binaryeye.net

import android.net.Uri

private const val SCHEME = "binaryeye"
private const val HOSTNAME = "markusfisch.de"
private const val ENCODE_PATH = "encode"

fun isScanDeeplink(text: String) = listOf(
	"$SCHEME://scan",
	"http://$HOSTNAME/BinaryEye",
	"https://$HOSTNAME/BinaryEye"
).any { text.startsWith(it) }

fun isEncodeDeeplink(text: String) = listOf(
	"$SCHEME://$ENCODE_PATH",
	"http://$HOSTNAME/$ENCODE_PATH",
	"https://$HOSTNAME/$ENCODE_PATH"
).any { text.startsWith(it) }

fun createEncodeDeeplink(format: String, content: String): String {
	return Uri.Builder()
		.scheme("https")
		.authority(HOSTNAME)
		.appendPath(ENCODE_PATH)
		.appendQueryParameter("format", format)
		.appendQueryParameter("content", content)
		.build()
			.toString()
}
