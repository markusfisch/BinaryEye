package de.markusfisch.android.binaryeye.app

import java.util.regex.Pattern

private val nonPrintable = Pattern.compile("[\\x00-\\x08\\x0e-\\x1f]")

fun hasNonPrintableCharacters(s: String) = nonPrintable.matcher(s).find()
