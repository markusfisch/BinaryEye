package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.widget.Toast
import de.markusfisch.android.binaryeye.app.ellipsize

fun Context.toast(message: Int) = Toast.makeText(
	applicationContext,
	message,
	Toast.LENGTH_LONG
).show()

fun Context.toast(message: String) = Toast.makeText(
	applicationContext,
	message.ellipsize(128),
	Toast.LENGTH_LONG
).show()