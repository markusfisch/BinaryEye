package de.markusfisch.android.binaryeye.activity

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.app.AppCompatActivity
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.PERMISSION_LOCATION
import de.markusfisch.android.binaryeye.app.PERMISSION_WRITE
import de.markusfisch.android.binaryeye.app.applyLocale
import de.markusfisch.android.binaryeye.app.permissionGrantedCallback
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.view.initBars

abstract class ScreenActivity : AppCompatActivity() {
	override fun attachBaseContext(base: Context?) {
		base?.applyLocale(prefs.customLocale)
		super.attachBaseContext(base)
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_main)
		initBars()
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
		super.onRequestPermissionsResult(
			requestCode,
			permissions,
			grantResults
		)
		when (requestCode) {
			PERMISSION_LOCATION, PERMISSION_WRITE -> {
				if (grantResults.isNotEmpty() &&
					grantResults[0] == PackageManager.PERMISSION_GRANTED
				) {
					permissionGrantedCallback?.invoke()
					permissionGrantedCallback = null
				}
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		onCreateOptionsMenu(menu, menuInflater)
		return menu.size() > 0 || super.onCreateOptionsMenu(menu)
	}

	protected open fun onCreateOptionsMenu(
		menu: Menu,
		inflater: MenuInflater
	) {
	}
}
