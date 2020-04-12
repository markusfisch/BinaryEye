package de.markusfisch.android.binaryeye.view

import android.graphics.Rect
import android.os.Build
import android.support.v4.view.ViewCompat
import android.support.v7.widget.Toolbar
import android.view.View

private val windowInsets = Rect()
private var windowInsetsSet = false
private var windowInsetsListener: ((insets: Rect) -> Unit)? = null

// invoke this from Fragment.onCreateView() *or* Activity.onCreate()
fun setWindowInsetListener(listener: (insets: Rect) -> Unit) {
	if (windowInsetsSet) {
		// invoke listener for previously set window insets because
		// setOnApplyWindowInsetsListener() won't fire again if we
		// change Fragments
		listener(windowInsets)
	}
	// *always* set the listener in case there is a configuration change
	windowInsetsListener = listener
}

// invoke this from Activity.onCreate() to setup listening for insets
fun setupInsets(view: View, toolbar: Toolbar) {
	val toolBarHeight = toolbar.layoutParams.height
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
		// save insets so setWindowInsetListener() can return them
		// when it's called from Fragment.onCreateView() later on
		windowInsets.set(0, toolBarHeight, 0, 0)
		windowInsetsSet = true
		windowInsetsListener?.also { it(windowInsets) }
		return
	}
	ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
		// runs *after* layout and thus *after* Fragment.onCreateView()
		if (insets.hasSystemWindowInsets()) {
			windowInsets.set(
				insets.systemWindowInsetLeft,
				insets.systemWindowInsetTop + toolBarHeight,
				insets.systemWindowInsetRight,
				insets.systemWindowInsetBottom
			)
			windowInsetsSet = true
			windowInsetsListener?.also { it(windowInsets) }
		}
		insets
	}
}
