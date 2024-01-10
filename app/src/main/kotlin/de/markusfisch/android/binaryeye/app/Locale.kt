package de.markusfisch.android.binaryeye.app

import android.content.Context
import android.os.Build
import java.util.Locale

fun Context.applyLocale(localeName: String) {
	if (localeName.isEmpty()) {
		return
	}
	val localeParts = localeName.split("-")
	val locale = if (localeParts.size == 2) {
		Locale(localeParts[0], localeParts[1])
	} else {
		Locale(localeName)
	}
	Locale.setDefault(locale)
	val conf = resources.configuration
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
		@Suppress("DEPRECATION")
		conf.locale = locale
	} else {
		conf.setLocale(locale)
	}
	resources.updateConfiguration(conf, resources.displayMetrics)
}
