package de.markusfisch.android.binaryeye.app

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend inline fun <T : Any> alertDialog(
	context: Context,
	crossinline build: MaterialAlertDialogBuilder.(resume: (T?) -> Unit) -> Unit
): T? = withContext(Dispatchers.Main) {
	suspendCoroutine { continuation ->
		MaterialAlertDialogBuilder(context).apply {
			setOnCancelListener {
				continuation.resume(null)
			}
			build(continuation::resume)
			show()
		}
	}
}
