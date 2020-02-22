package de.markusfisch.android.binaryeye.app

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.support.annotation.MainThread
import android.widget.EditText
import de.markusfisch.android.binaryeye.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.IOException

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

suspend fun Flow<ByteArray>.writeToFile(name: String): Int {
	return try {
		val file = File(
			Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS
			),
			name
		)
		if (file.exists()) {
			return R.string.error_file_exists
		}
		collect { bytes ->
			file.appendBytes(bytes)
		}
		R.string.saved_in_downloads
	} catch (e: IOException) {
		R.string.error_saving_binary_data
	}
}
