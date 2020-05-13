package de.markusfisch.android.binaryeye.app

import android.app.Activity
import android.content.Intent
import de.markusfisch.android.binaryeye.activity.SplashActivity

private const val RESTART_COUNT = "restart_count"

private var restartCount = 0

fun setRestartCount(intent: Intent?) {
	intent?.let {
		restartCount = intent.getIntExtra(RESTART_COUNT, restartCount)
	}
}

fun restartApp(activity: Activity? = null) {
	if (activity != null && restartCount < 3) {
		val intent = Intent(activity, SplashActivity::class.java)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		intent.putExtra(RESTART_COUNT, ++restartCount)
		activity.startActivity(intent)
		activity.finish()
	}
	Runtime.getRuntime().exit(0)
}
