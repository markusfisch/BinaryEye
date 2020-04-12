package de.markusfisch.android.binaryeye.view

import android.graphics.Rect
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend inline fun View.useVisibility(
	whileExecuting: Int = View.VISIBLE,
	otherwise: Int = View.GONE,
	crossinline block: suspend () -> Unit
) {
	if (visibility == whileExecuting) {
		return
	}
	withContext(Dispatchers.Main) {
		visibility = whileExecuting
	}
	try {
		block()
	} finally {
		withContext(Dispatchers.Main) {
			visibility = otherwise
		}
	}
}

fun View.setPadding(rect: Rect) {
	this.setPadding(
		rect.left,
		rect.top,
		rect.right,
		rect.bottom
	)
}
