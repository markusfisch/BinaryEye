package de.markusfisch.android.binaryeye.view

import android.content.Context
import android.content.res.Resources
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

	private fun getIdentifierDimen(res: Resources, name: String): Int {
		val id = res.getIdentifier(name, "dimen", "android")
		return if (id > 0) res.getDimensionPixelSize(id) else 0
	}
}
