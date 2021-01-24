package de.markusfisch.android.binaryeye.view

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

fun Context.hideSoftKeyboard(view: View) {
	(getSystemService(
		Context.INPUT_METHOD_SERVICE
	) as InputMethodManager).hideSoftInputFromWindow(
		view.windowToken, 0
	)
}