package de.markusfisch.android.binaryeye.app

import android.content.Context
import android.support.v7.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend inline fun <T : Any> alertDialog(
	context: Context,
	crossinline build: AlertDialog.Builder.(resume: (T?) -> Unit) -> Unit
): T? = withContext(Dispatchers.Main) {
	suspendCoroutine<T?> { continuation ->
		AlertDialog.Builder(context).apply {
			setOnCancelListener {
				continuation.resume(null)
			}
			build(continuation::resume)
			show()
		}
	}
}
