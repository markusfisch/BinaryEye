package de.markusfisch.android.binaryeye.view

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.util.TypedValue

object SystemBarMetrics {
	fun getStatusAndToolBarHeight(context: Context): Int {
		return getStatusBarHeight(context.resources) + getToolBarHeight(context)
	}

	fun getStatusBarHeight(res: Resources): Int {
		return getIdentifierDimen(res, "status_bar_height")
	}

	fun getToolBarHeight(context: Context): Int {
		val tv = TypedValue()
		return if (context.theme.resolveAttribute(
				android.R.attr.actionBarSize,
				tv,
				true))
			TypedValue.complexToDimensionPixelSize(
					tv.data,
					context.resources.displayMetrics)
		else
			0
	}

	fun getNavigationBarSize(res:Resources):Point {
		val size = Point(0, 0)
		if (!getIdentifierBoolean(res, "config_showNavigationBar")) {
			return size
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			val conf = res.getConfiguration()
			if (conf.orientation == Configuration.ORIENTATION_LANDSCAPE &&
					// according to https://developer.android.com/training/multiscreen/screensizes.html#TaskUseSWQuali
					// only a screen < 600 dp is considered to be a phone
					// and can move its navigation bar to the side
					conf.smallestScreenWidthDp < 600) {
				size.x = getIdentifierDimen(res,
						"navigation_bar_height_landscape")
				return size
			}
		}
		size.y = getIdentifierDimen(res, "navigation_bar_height")
		return size
	}

	private fun getIdentifierBoolean(res:Resources, name:String):Boolean {
		val id = res.getIdentifier(name, "bool", "android")
		return id > 0 && res.getBoolean(id)
	}

	private fun getIdentifierDimen(res: Resources, name: String): Int {
		val id = res.getIdentifier(name, "dimen", "android")
		return if (id > 0) res.getDimensionPixelSize(id) else 0
	}
}
