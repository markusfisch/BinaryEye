package de.markusfisch.android.binaryeye.app

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.MainThread
import android.widget.EditText
import de.markusfisch.android.binaryeye.R
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

fun writeExternalFile(
	context: Context,
	fileName: String,
	mimeType: String,
	write: (outputStream: OutputStream) -> Any
): Int = try {
	val outputStream = getExternalOutputStream(
		context,
		fileName,
		mimeType
	) ?: throw IOException()
	outputStream.use { write(it) }
	R.string.saved_in_downloads
} catch (e: FileAlreadyExistsException) {
	R.string.error_file_exists
} catch (e: IOException) {
	R.string.error_saving_binary_data
}

private fun getExternalOutputStream(
	context: Context,
	fileName: String,
	mimeType: String
): OutputStream? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
	@Suppress("DEPRECATION")
	val file = File(
		Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_DOWNLOADS
		),
		fileName
	)
	if (file.exists()) {
		throw FileAlreadyExistsException(file)
	}
	FileOutputStream(file)
} else {
	val resolver = context.contentResolver
	val uri = resolver.insert(
		MediaStore.Downloads.EXTERNAL_CONTENT_URI,
		ContentValues().apply {
			put(MediaStore.Downloads.DISPLAY_NAME, fileName)
			put(MediaStore.Downloads.MIME_TYPE, mimeType)
		}
	)
	if (uri != null) {
		resolver.openOutputStream(uri)
	} else {
		null
	}
}
