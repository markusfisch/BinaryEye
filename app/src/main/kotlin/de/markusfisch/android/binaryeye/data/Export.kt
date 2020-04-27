package de.markusfisch.android.binaryeye.data

import android.app.Activity
import android.os.Environment
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.writeExternalFile
import de.markusfisch.android.binaryeye.widget.toast
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

fun exportDatabase(activity: Activity, fileName: String) {
	if (!hasWritePermission(activity)) {
		return
	}
	val dbFile = File(
		Environment.getDataDirectory(),
		"//data//${activity.packageName}//databases//${Database.FILE_NAME}"
	)
	if (!dbFile.exists()) {
		activity.toast(R.string.error_no_content)
		return
	}
	GlobalScope.launch {
		val message = writeExternalFile(
			activity,
			fileName,
			"application/vnd.sqlite3"
		) {
			FileInputStream(dbFile).copyTo(it)
		}
		GlobalScope.launch(Main) {
			activity.toast(message)
		}
	}
}
