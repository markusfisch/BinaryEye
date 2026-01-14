package de.markusfisch.android.binaryeye.content

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v4.content.FileProvider
import de.markusfisch.android.binaryeye.BuildConfig
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.widget.toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun Context.execShareIntent(intent: Intent): Boolean = if (
	!startIntent(intent)
) {
	toast(R.string.cannot_resolve_action)
	false
} else {
	true
}

fun Context.startIntent(intent: Intent): Boolean = try {
	// Avoid using `intent.resolveActivity()` at API level 30+ due
	// to the new package visibility restrictions. In order for
	// `resolveActivity()` to "see" another package, we would need
	// to list that package/intent in a `<queries>` block in the
	// Manifest. But since we used `resolveActivity()` only to avoid
	// an exception if the Intent cannot be resolved, it's much easier
	// and more robust to just try and catch that exception if
	// necessary.
	startActivity(intent)
	true
} catch (_: ActivityNotFoundException) {
	false
}

fun Context.openUrl(url: String, silent: Boolean = false): Boolean {
	return openUri(url.parseAndNormalizeUri(), silent)
}

fun Context.openUri(uri: Uri, silent: Boolean = false): Boolean {
	val intent = Intent(Intent.ACTION_VIEW, uri).apply {
		if (uri.scheme == "content") {
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}
	}
	return if (silent) {
		startIntent(intent)
	} else {
		execShareIntent(intent)
	}
}

fun String.parseAndNormalizeUri(): Uri = Uri.parse(this).let {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
		it.normalizeScheme()
	} else {
		it
	}
}

fun Context.shareText(text: String, mimeType: String = "text/plain") {
	execShareIntent(Intent(Intent.ACTION_SEND).apply {
		putExtra(Intent.EXTRA_TEXT, text)
		type = mimeType
	})
}

private var lastShareFile: File? = null
fun wipeLastShareFile() {
	lastShareFile?.let {
		it.delete()
		lastShareFile = null
	}
}

fun <T> Context.shareAsFile(content: T, fileName: String) {
	val (bytes, mimeType) = when (content) {
		is ByteArray -> Pair(
			content,
			"text/plain"
		)

		is String -> Pair(
			content.toByteArray(),
			"application/octet-stream"
		)

		else -> throw IllegalArgumentException(
			"Unsupported content text for shareAsFile()"
		)
	}
	wipeLastShareFile()
	val file = File(externalCacheDir, fileName)
	try {
		FileOutputStream(file).use {
			it.write(bytes)
		}
	} catch (_: IOException) {
		toast(R.string.error_saving_file)
		return
	}
	lastShareFile = file
	shareFile(file, mimeType)
}

fun Context.shareFile(file: File, mimeType: String) {
	getUriForFile(file)?.let {
		shareUri(it, mimeType)
	}
}

private fun Context.getUriForFile(file: File): Uri? = if (
	Build.VERSION.SDK_INT < Build.VERSION_CODES.N
) {
	Uri.fromFile(file)
} else {
	FileProvider.getUriForFile(
		this,
		BuildConfig.APPLICATION_ID + ".provider",
		file
	)
}

private fun Context.shareUri(uri: Uri, mimeType: String) {
	execShareIntent(Intent(Intent.ACTION_SEND).apply {
		putExtra(Intent.EXTRA_STREAM, uri)
		addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		type = mimeType
	})
}
