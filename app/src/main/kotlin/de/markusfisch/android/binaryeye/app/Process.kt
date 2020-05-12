package de.markusfisch.android.binaryeye.app

import android.app.Activity
import android.content.Intent
import de.markusfisch.android.binaryeye.activity.SplashActivity

fun restartApp(activity: Activity? = null) {
	activity?.let {
		val intent = Intent(it, SplashActivity::class.java)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		it.startActivity(intent)
		it.finish()
	}
	Runtime.getRuntime().exit(0)
}
