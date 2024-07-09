package de.markusfisch.android.binaryeye.os

import android.app.Activity
import android.view.WindowManager

fun Activity.getScreenBrightness(): Float {
	val layoutParams = window.attributes
	return if (layoutParams.screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
		android.provider.Settings.System.getInt(
			contentResolver,
			android.provider.Settings.System.SCREEN_BRIGHTNESS
		) / 255.0f
	} else {
		layoutParams.screenBrightness
	}
}

fun Activity.setScreenBrightness(brightness: Float) {
	val layoutParams = window.attributes
	layoutParams.screenBrightness = brightness
	window.attributes = layoutParams
}
