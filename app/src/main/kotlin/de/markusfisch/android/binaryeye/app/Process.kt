package de.markusfisch.android.binaryeye.app

import android.app.Activity
import android.content.Intent
import de.markusfisch.android.binaryeye.activity.SplashActivity

private const val RESTART_COUNT = "restart_count"

private var restartCount = 0

fun Intent.setRestartCount() {
	restartCount = getIntExtra(RESTART_COUNT, restartCount)
}

fun Activity.restartApp() {
	if (restartCount < 3) {
		startActivity(Intent(this, SplashActivity::class.java).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			putExtra(RESTART_COUNT, ++restartCount)
		})
		finish()
	}
	Runtime.getRuntime().exit(0)
}
