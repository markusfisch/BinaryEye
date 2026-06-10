package de.markusfisch.android.binaryeye.view

import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

private var toolbarHeight = 0
fun setPaddingFromWindowInsets(
	mainLayout: View,
	toolbar: Toolbar,
	navbar: View?
) {
	toolbarHeight = toolbar.layoutParams.height
	// SDK 35+ is edge-to-edge by default, so the toolbar needs to extend
	// below the status bar.
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
		return
	}
	// Capture the bare toolbar height so the listener can recompute the
	// padded height from scratch every time. Applying the insets only
	// once (or adding them incrementally) leaves the layout stuck with
	// stale values when the first callback reports wrong insets, which
	// is why some devices only settled after several rotations.
	val baseToolbarHeight = toolbarHeight
	mainLayout.setOnApplyWindowInsetsListener { _, insets ->
		val systemBarInsets = insets.getInsets(
			WindowInsets.Type.systemBars()
		)

		val statusBarTop = systemBarInsets.top
		toolbar.setPadding(0, statusBarTop, 0, 0)
		toolbar.layoutParams.height = baseToolbarHeight + statusBarTop

		val navBarBottom = systemBarInsets.bottom
		mainLayout.setPadding(0, 0, 0, navBarBottom)
		navbar?.apply {
			layoutParams.height = navBarBottom
		}

		insets
	}
}

fun View.setPaddingFromWindowInsets(bottom: Boolean = true) {
	doOnApplyWindowInsets { v, insets, windowInsets ->
		if (!bottom) {
			insets.bottom = getImeBottomInsetWithoutSystemBars(windowInsets)
		}
		v.setPadding(insets)
	}
}

// A slight variation of the idea from Google's Chris Banes to avoid
// adding the toolbar height for every view that needs to be inset.
// For the original post see here:
// https://medium.com/androiddevelopers/windowinsets-listeners-to-layouts-8f9ccc8fa4d1
fun View.doOnApplyWindowInsets(f: (View, Rect, WindowInsetsCompat) -> Unit) {
	ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
		f(v, insetsWithToolbar(insets), insets)
		insets
	}
	// It's important to explicitly request the insets (again) in
	// case the view was created in Fragment.onCreateView() because
	// setOnApplyWindowInsetsListener() won't fire when the view
	// isn't attached.
	requestApplyInsetsWhenAttached()
}

private fun insetsWithToolbar(insets: WindowInsetsCompat? = null) = Rect(
	insets?.systemWindowInsetLeft ?: 0,
	(insets?.systemWindowInsetTop ?: 0) + toolbarHeight,
	insets?.systemWindowInsetRight ?: 0,
	insets?.systemWindowInsetBottom ?: 0
)

private fun getImeBottomInsetWithoutSystemBars(insets: WindowInsetsCompat) =
	(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom -
			insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
		.coerceAtLeast(0)

private fun View.requestApplyInsetsWhenAttached() {
	if (isAttachedToWindow) {
		ViewCompat.requestApplyInsets(this)
	} else {
		addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
			override fun onViewAttachedToWindow(v: View) {
				v.removeOnAttachStateChangeListener(this)
				ViewCompat.requestApplyInsets(v)
			}

			override fun onViewDetachedFromWindow(v: View) = Unit
		})
	}
}
