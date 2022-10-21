package de.markusfisch.android.binaryeye.content

fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }