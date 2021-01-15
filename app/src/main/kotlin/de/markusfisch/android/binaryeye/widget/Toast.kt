package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.widget.Toast

fun Context.toast(message: Int) = Toast.makeText(
	applicationContext,
	message,
	Toast.LENGTH_LONG
).show()

fun Context.toast(message: String) = Toast.makeText(
	applicationContext,
	message,
	Toast.LENGTH_LONG
).show()
