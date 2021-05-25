package de.markusfisch.android.binaryeye.view

import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.WindowManager
import android.widget.AbsListView
import de.markusfisch.android.binaryeye.R

val systemBarListViewScrollListener = object : AbsListView.OnScrollListener {
	override fun onScroll(
		view: AbsListView,
		firstVisibleItem: Int,
		visibleItemCount: Int,
		totalItemCount: Int
	) {
		// Give Android some time to settle down before running this,
		// not putting it on the queue makes it only work sometimes.
		view.post {
			val scrolled = firstVisibleItem > 0 ||
					(totalItemCount > 0 && firstChildScrolled(view))
			val scrollable = scrolled || totalItemCount > 0 && lastChildOutOfView(view)
			colorSystemAndToolBars(view.context, scrolled, scrollable)
		}
	}

	override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
	}
}

val systemBarRecyclerViewScrollListener = object : RecyclerView.OnScrollListener() {
	override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
		val layoutManager = recyclerView.layoutManager as LinearLayoutManager
		val scrolled = layoutManager.findFirstCompletelyVisibleItemPosition() != 0
		val scrollable = scrolled || layoutManager.findLastVisibleItemPosition() <
				recyclerView.adapter.itemCount - 1
		colorSystemAndToolBars(recyclerView.context, scrolled, scrollable)
	}

	override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
	}
}

private fun firstChildScrolled(listView: AbsListView): Boolean {
	val child = listView.getChildAt(0)
	return child != null && child.top < listView.paddingTop
}

private fun lastChildOutOfView(listView: AbsListView): Boolean {
	val child = listView.getChildAt(listView.lastVisiblePosition)
	return child != null && child.bottom >= listView.height
}

fun initSystemBars(activity: AppCompatActivity) {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		// Keeps the soft keyboard from repositioning the layout.
		val window = activity.window
		window.setSoftInputMode(
			WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
		)
		window.decorView.systemUiVisibility =
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	}
	colorSystemAndToolBars(activity)
}

private var statusBarColorLocked = false
fun lockStatusBarColor() {
	statusBarColorLocked = true
}

fun unlockStatusBarColor() {
	statusBarColorLocked = false
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
		if (!statusBarColorLocked) {
			window.statusBarColor = topColor
		}
		window.navigationBarColor = if (scrolled || scrollable) {
			translucentPrimaryColor
		} else {
			0
		}
	}
	activity.supportActionBar?.setBackgroundDrawable(
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Avoid allocation on Honeycomb and better.
			actionBarBackground.color = topColor
			actionBarBackground
		} else {
			// ColorDrawable.setColor() doesn't exist pre Honeycomb.
			ColorDrawable(topColor)
		}
	)
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
