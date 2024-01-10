package de.markusfisch.android.binaryeye.io

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.MainThread
import android.widget.EditText
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.alertDialog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

fun addSuffixIfNotGiven(name: String, suffix: String): String {
	val trimmed = name.trim()
	return if (suffix.isNotEmpty() && !trimmed.endsWith(suffix)) {
		"$trimmed$suffix"
	} else {
		trimmed
	}
}

// Dialogs doesn't have any root layout, need to be inflated with null root
@Suppress("InflateParams")
@MainThread
suspend fun Activity.askForFileName(suffix: String = ""): String? {
	val view = layoutInflater.inflate(R.layout.dialog_save_file, null)
	val editText = view.findViewById<EditText>(R.id.file_name)
	return alertDialog<String>(this as Context) { resume ->
		setView(view)
		setPositiveButton(android.R.string.ok) { _, _ ->
			resume(editText.text.toString())
		}
	}?.let { name ->
		addSuffixIfNotGiven(name, suffix)
	}
}

fun Boolean.toSaveResult() = if (this) {
	R.string.saved_in_downloads
} else {
	R.string.error_saving_file
}

fun Context.writeExternalFile(
	fileName: String,
	mimeType: String,
	write: (outputStream: OutputStream) -> Unit
): Boolean = try {
	openExternalOutputStream(fileName, mimeType).use { write(it) }
	true
} catch (e: IOException) {
	false
}

private fun Context.openExternalOutputStream(
	fileName: String,
	mimeType: String
): OutputStream = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
	val file = File(
		Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_DOWNLOADS
		),
		fileName
	)
	if (file.exists()) {
		throw IOException()
	}
	FileOutputStream(file)
} else {
	val resolver = contentResolver
	val uri = resolver.insert(
		MediaStore.Downloads.EXTERNAL_CONTENT_URI,
		ContentValues().apply {
			put(MediaStore.Downloads.DISPLAY_NAME, fileName)
			put(MediaStore.Downloads.MIME_TYPE, mimeType)
		}
	) ?: throw IOException()
	resolver.openOutputStream(uri) ?: throw IOException()
}
