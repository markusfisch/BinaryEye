package de.markusfisch.android.binaryeye.app

import de.markusfisch.android.binaryeye.R

import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window

fun initSystemBars(activity: AppCompatActivity?) {
	val view = activity?.findViewById(R.id.main_layout) ?: return
	setWindowInsetListener(view)
	setSystemBarColor(
		activity.window,
		ContextCompat.getColor(activity, android.R.color.transparent)
	)
}

fun setWindowInsetListener(view: View) {
	ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
		if (insets.hasSystemWindowInsets()) {
			view.setPadding(
				insets.getSystemWindowInsetLeft(),
				insets.getSystemWindowInsetTop(),
				insets.getSystemWindowInsetRight(),
				insets.getSystemWindowInsetBottom()
			)
		}
		insets.consumeSystemWindowInsets()
	}
}

fun setSystemBarColor(window: Window, color: Int): Boolean {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
		return false
	}
	window.statusBarColor = color
	window.navigationBarColor = color
	window.decorView.systemUiVisibility =
		View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
		View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
		View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	return true
}
