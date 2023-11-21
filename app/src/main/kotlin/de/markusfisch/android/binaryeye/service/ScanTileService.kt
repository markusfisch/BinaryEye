package de.markusfisch.android.binaryeye.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import android.support.annotation.RequiresApi
import de.markusfisch.android.binaryeye.activity.CameraActivity

@RequiresApi(Build.VERSION_CODES.N)
class ScanTileService : TileService() {
	override fun onClick() {
		super.onClick()
		val intent = Intent(applicationContext, CameraActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			@Suppress("DEPRECATION")
			startActivityAndCollapse(intent)
		} else {
			val pendingIntent = PendingIntent.getActivity(
				this,
				0,
				intent,
				PendingIntent.FLAG_IMMUTABLE
			)
			startActivityAndCollapse(pendingIntent)
		}
	}
}
