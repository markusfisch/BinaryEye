package de.markusfisch.android.binaryeye.app

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.support.annotation.MainThread
import android.widget.EditText
import de.markusfisch.android.binaryeye.R
import java.io.File
import java.io.IOException

@Suppress("InflateParams") // Dialogs doesn't have any root layout, need to be inflated with null root
@MainThread
suspend fun Activity.askForFileName(suffix: String? = null): String? {
	val view = layoutInflater.inflate(R.layout.dialog_save_file, null)
	val editText = view.findViewById<EditText>(R.id.file_name)
	return alertDialog<String>(this as Context) { resume ->
		setView(view)
		setPositiveButton(android.R.string.ok) { _, _ ->
			resume(editText.text.toString())
		}
	}?.let { name ->
		if (suffix != null && !name.endsWith(".$suffix")) {
			"${name.trim()}.$suffix"
		} else {
			name.trim()
		}
	}
}

fun saveByteArray(name: String, raw: ByteArray): Int {
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
		file.writeBytes(raw)
		R.string.saved_in_downloads
	} catch (e: IOException) {
		R.string.error_saving_binary_data
	}
}
