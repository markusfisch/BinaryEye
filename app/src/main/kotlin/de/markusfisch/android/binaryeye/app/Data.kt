package de.markusfisch.android.binaryeye.app

import android.net.Uri
import android.os.Build
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

private val nonPrintable = "[\\x00-\\x08\\x0e-\\x1f]".toRegex()

fun String.hasNonPrintableCharacters() = nonPrintable.containsMatchIn(this)

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
