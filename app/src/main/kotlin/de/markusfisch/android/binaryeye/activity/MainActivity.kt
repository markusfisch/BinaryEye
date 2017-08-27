package de.markusfisch.android.binaryeye.activity

import de.markusfisch.android.cameraview.widget.CameraView

import de.markusfisch.android.binaryeye.fragment.CameraFragment
import de.markusfisch.android.binaryeye.view.SystemBarMetrics
import de.markusfisch.android.binaryeye.widget.LockOnView
import de.markusfisch.android.binaryeye.R

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.Window
import android.widget.Toast

class MainActivity : AppCompatActivity() {
	lateinit var cameraView: CameraView
	lateinit var lockOnView: LockOnView

	override fun onRequestPermissionsResult(
			requestCode: Int,
			permissions: Array<String>,
			grantResults: IntArray) {
		when (requestCode) {
			REQUEST_CAMERA -> if (grantResults.size > 0 &&
					grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(this,
						R.string.no_camera_no_fun,
						Toast.LENGTH_SHORT).show()
				finish()
			}
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		val fm = supportFragmentManager
		if (fm != null && fm.backStackEntryCount > 0) {
			fm.popBackStack()
		} else {
			finish()
		}
		return true
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_main)
		initSystemBars(this)

		cameraView = findViewById(R.id.camera_view) as CameraView
		lockOnView = findViewById(R.id.lock_on) as LockOnView

		setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)
		setUpListener()
		checkPermissions()

		if (state == null) {
			supportFragmentManager
					.beginTransaction()
					.replace(R.id.content_frame, CameraFragment())
					.commit()
		}
	}

	private fun checkPermissions() {
		val permission = android.Manifest.permission.CAMERA

		if (ContextCompat.checkSelfPermission(this, permission) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, arrayOf(permission),
					REQUEST_CAMERA)
		}
	}

	private fun setUpListener() {
		supportFragmentManager.addOnBackStackChangedListener { canBack() }
		canBack()
	}

	private fun canBack() {
		supportActionBar?.setDisplayHomeAsUpEnabled(
				supportFragmentManager.backStackEntryCount > 0)
	}

	companion object {
		private val REQUEST_CAMERA = 1

		private fun initSystemBars(activity: AppCompatActivity?) {
			val view = activity?.findViewById(R.id.main_layout)
			if (view != null && setSystemBarColor(activity.window,
					ContextCompat.getColor(activity,
							R.color.primary_dark_translucent))) {
				view.setPadding(
						0,
						SystemBarMetrics.getStatusBarHeight(
								activity.resources),
						0,
						0)
			}
		}

		private fun setSystemBarColor(window: Window, color: Int): Boolean {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
				return false
			}

			window.statusBarColor = color
			window.navigationBarColor = color
			window.decorView.systemUiVisibility =
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

			return true
		}
	}
}
