package de.markusfisch.android.binaryeye.app

import de.markusfisch.android.binaryeye.BuildConfig

import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.content.FileProvider
import android.net.Uri

import java.io.File

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

fun shareFile(context: Context, file: File, type: String) {
	shareUri(context, getUriForFile(context, file), type)
}

fun getUriForFile(context: Context, file: File): Uri {
	return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
		Uri.fromFile(file)
	} else {
		FileProvider.getUriForFile(
			context,
			BuildConfig.APPLICATION_ID + ".provider",
			file
		)
	}
}
