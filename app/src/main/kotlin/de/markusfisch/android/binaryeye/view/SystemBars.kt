package de.markusfisch.android.binaryeye.view

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.AbsListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
			val scrollable = scrolled ||
					totalItemCount > 0 && lastChildOutOfView(view)
			colorSystemAndToolBars(view.context, scrolled, scrollable)
		}
	}

	override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
	}
}

val systemBarRecyclerViewScrollListener = object : RecyclerView.OnScrollListener() {
	override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
		val layoutManager = recyclerView.layoutManager as LinearLayoutManager
		val scrolled =
			layoutManager.findFirstCompletelyVisibleItemPosition() != 0
		val scrollable = scrolled ||
				layoutManager.findLastVisibleItemPosition() <
				(recyclerView.adapter?.itemCount ?: 0) - 1
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

fun AppCompatActivity.initBars() {
	// Keeps the soft keyboard from repositioning the layout.
	window.setSoftInputMode(
		WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
	)
	window.decorView.systemUiVisibility =
		View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	colorSystemAndToolBars(this)
	setPaddingFromWindowInsets(
		findViewById(R.id.main),
		(findViewById(R.id.toolbar) as Toolbar).apply {
			setSupportActionBar(this)
		},
		findViewById<View>(R.id.navbar)?.apply {
			setBackgroundColor(translucentPrimaryColor)
		}
	)
}

private var statusBarColorLocked = false
fun lockStatusBarColor() {
	statusBarColorLocked = true
}

fun unlockStatusBarColor() {
	statusBarColorLocked = false
}

private var translucentPrimaryColor = 0
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
	// System bars no longer have a background from SDK35+.
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
		val window = activity.window
		if (!statusBarColorLocked) {
			@Suppress("DEPRECATION")
			window.statusBarColor = topColor
		}
		@Suppress("DEPRECATION")
		window.navigationBarColor = if (scrolled || scrollable) {
			translucentPrimaryColor
		} else {
			0
		}
	} else {
		activity.findViewById<View>(R.id.navbar)?.apply {
			visibility = if (scrolled || scrollable) {
				View.VISIBLE
			} else {
				View.GONE
			}
		}
	}
	activity.supportActionBar?.setBackgroundDrawable(topColor.toDrawable())
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
