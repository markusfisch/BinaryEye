package de.markusfisch.android.binaryeye.app

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import android.widget.AbsListView
import de.markusfisch.android.binaryeye.R

val systemBarScrollListener = object : AbsListView.OnScrollListener {
	override fun onScroll(
		view: AbsListView,
		firstVisibleItem: Int,
		visibleItemCount: Int,
		totalItemCount: Int
	) {
		view.post({
			setSystemAndToolBarTransparency(
				view.context,
				firstVisibleItem > 0 || (totalItemCount > 0 && (
						view.getChildAt(view.lastVisiblePosition).bottom >
								view.height || view.getChildAt(0).top < 0))
			)
		})
	}

	override fun onScrollStateChanged(
		view: AbsListView,
		scrollState: Int
	) {
	}
}

fun setSystemAndToolBarTransparency(context: Context, opaque: Boolean = false) {
	val color = ContextCompat.getColor(
		context,
		if (opaque) {
			R.color.primary
		} else {
			android.R.color.transparent
		}
	)
	val activity = context as AppCompatActivity
	setSystemBarColor(activity.window, color)
	activity.supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
}

private fun setSystemBarColor(window: Window, color: Int) {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		window.statusBarColor = color
		window.navigationBarColor = color
	}
}

fun initSystemBars(activity: AppCompatActivity?) {
	val view = activity?.findViewById(R.id.main_layout) ?: return
	setWindowInsetListener(view)
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		activity.window.decorView.systemUiVisibility =
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	}
	setSystemAndToolBarTransparency(activity)
}

private fun setWindowInsetListener(view: View) {
	ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
		if (insets.hasSystemWindowInsets()) {
			view.setPadding(
				insets.systemWindowInsetLeft,
				insets.systemWindowInsetTop,
				insets.systemWindowInsetRight,
				insets.systemWindowInsetBottom
			)
		}
		insets.consumeSystemWindowInsets()
	}
}
