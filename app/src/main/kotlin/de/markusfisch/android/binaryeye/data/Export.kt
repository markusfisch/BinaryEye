package de.markusfisch.android.binaryeye.data

import android.app.Activity
import android.os.Environment
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.writeExternalFile
import java.io.File
import java.io.FileInputStream

fun exportDatabase(activity: Activity, fileName: String): Boolean {
	if (!hasWritePermission(activity)) {
		return false
	}
	val dbFile = File(
		Environment.getDataDirectory(),
		"//data//${activity.packageName}//databases//${Database.FILE_NAME}"
	)
	if (!dbFile.exists()) {
		return false
	}
	return writeExternalFile(
		activity,
		fileName,
		"application/vnd.sqlite3"
	) {
		FileInputStream(dbFile).copyTo(it)
	}
}
