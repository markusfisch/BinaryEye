package de.markusfisch.android.binaryeye.app

import android.net.Uri
import android.os.Build
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

private val nonAlNum = "[^a-zA-Z0-9]".toRegex()
private val multipleDots = "[…]+".toRegex()
fun String.foldNonAlNum() = replace(nonAlNum, "…")
	.replace(multipleDots, "…")

fun String.urlEncode(): String = try {
	URLEncoder.encode(this, "UTF-8")
} catch (e: UnsupportedEncodingException) {
	this
}

fun String.parseAndNormalizeUri(): Uri = Uri.parse(this).let {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
		it.normalizeScheme()
	} else {
		it
	}
}

fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }

fun String.ellipsize(max: Int) = if (length < max) {
	this
} else {
	"${take(max)}…"
}
