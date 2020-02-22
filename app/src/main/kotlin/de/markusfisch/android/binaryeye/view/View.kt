package de.markusfisch.android.binaryeye.view

import android.graphics.Rect
import android.view.View

fun View.setPadding(rect: Rect) {
	this.setPadding(
		rect.left,
		rect.top,
		rect.right,
		rect.bottom
	)
}
