package de.markusfisch.android.binaryeye.app

import android.content.Context
import android.content.Intent
import android.net.Uri

fun shareText(context: Context, text: String, type: String = "text/plain") {
	val intent = Intent(Intent.ACTION_SEND)
	intent.putExtra(Intent.EXTRA_TEXT, text)
	intent.type = type
	context.startActivity(intent)
}

fun shareUri(context: Context, uri: Uri, type: String) {
	val intent = Intent(Intent.ACTION_SEND)
	intent.putExtra(Intent.EXTRA_STREAM, uri)
	intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
	intent.type = type
	context.startActivity(intent)
}
