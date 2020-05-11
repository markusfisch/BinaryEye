package de.markusfisch.android.binaryeye.app

import android.net.Uri
import android.os.Build

private val nonPrintable = "[\\x00-\\x08\\x0e-\\x1f]".toRegex()

fun hasNonPrintableCharacters(s: String) = nonPrintable.containsMatchIn(s)

fun parseAndNormalizeUri(input: String): Uri = Uri.parse(input).let {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) it.normalizeScheme() else it
}

fun ByteArray.toHexString(): String {
	val hex = StringBuilder()
	for (i in this.indices) {
		hex.append(String.format("%02X", this[i]))
	}
	return hex.toString()
}
