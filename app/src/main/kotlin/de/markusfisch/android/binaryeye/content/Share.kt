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

fun Context.execShareIntent(intent: Intent) {
	// Avoid using `intent.resolveActivity()` at API level 30+ due
	// to the new package visibility restrictions. In order for
	// `resolveActivity()` to "see" another package, we would need
	// to list that package/intent in a `<queries>` block in the
	// Manifest. But since we used `resolveActivity()` only to avoid
	// an exception if the Intent cannot be resolved, it's much easier
	// and more robust to just try and catch that exception if
	// necessary.
	try {
		startActivity(intent)
	} catch (e: ActivityNotFoundException) {
		toast(R.string.cannot_resolve_action)
	}
}

fun shareText(context: Context, text: String, type: String = "text/plain") {
	val intent = Intent(Intent.ACTION_SEND)
	intent.putExtra(Intent.EXTRA_TEXT, text)
	intent.type = type
	context.execShareIntent(intent)
}

fun shareUri(context: Context, uri: Uri, type: String) {
	val intent = Intent(Intent.ACTION_SEND)
	intent.putExtra(Intent.EXTRA_STREAM, uri)
	intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
	intent.type = type
	context.execShareIntent(intent)
}

fun shareFile(context: Context, file: File, type: String) {
	getUriForFile(context, file)?.let {
		shareUri(context, it, type)
	}
}

fun getUriForFile(context: Context, file: File): Uri? {
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
