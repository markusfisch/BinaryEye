package de.markusfisch.android.binaryeye.app

import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AbsListView
import de.markusfisch.android.binaryeye.R

val systemBarScrollListener = object : AbsListView.OnScrollListener {
	override fun onScroll(
		view: AbsListView,
		firstVisibleItem: Int,
		visibleItemCount: Int,
		totalItemCount: Int
	) {
		// give Android some time to settle down before running this,
		// not putting it on the queue makes it only work sometimes
		view.post {
			val scrolled = firstVisibleItem > 0 ||
					(totalItemCount > 0 && view.getChildAt(0).top < view.paddingTop)
			val scrollable = if (scrolled) true else totalItemCount > 0 &&
					view.getChildAt(view.lastVisiblePosition).bottom >= view.height
			colorSystemAndToolBars(view.context, scrolled, scrollable)
		}
	}

	override fun onScrollStateChanged(
		view: AbsListView,
		scrollState: Int
	) {
	}
}

fun initSystemBars(activity: AppCompatActivity) {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		activity.window.decorView.systemUiVisibility =
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	}
	colorSystemAndToolBars(activity)
}

private var translucentPrimaryColor = 0
private val actionBarBackground = ColorDrawable()
fun colorSystemAndToolBars(
	context: Context,
	scrolled: Boolean = false,
	scrollable: Boolean = false
) {
	if (translucentPrimaryColor == 0) {
		translucentPrimaryColor = ContextCompat.getColor(
			context,
			R.color.primary_translucent
		)
	}
	val topColor = if (scrolled) translucentPrimaryColor else 0
	val activity = getAppCompatActivity(context) ?: return
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		val window = activity.window
		window.statusBarColor = topColor
		window.navigationBarColor = if (scrolled || scrollable) {
			translucentPrimaryColor
		} else {
			0
		}
	}
	actionBarBackground.color = topColor
	activity.supportActionBar?.setBackgroundDrawable(actionBarBackground)
}

private fun getAppCompatActivity(context: Context): AppCompatActivity? {
	var ctx = context
	while (ctx is ContextWrapper) {
		if (ctx is AppCompatActivity) {
			return ctx
		}
		ctx = ctx.baseContext ?: break
	}
	return null
}
