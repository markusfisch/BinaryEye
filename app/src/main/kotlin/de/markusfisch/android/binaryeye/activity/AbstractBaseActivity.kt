package de.markusfisch.android.binaryeye.activity

import android.content.Context
import android.content.pm.PackageManager
import android.view.Menu
import android.view.MenuInflater
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isNotEmpty
import de.markusfisch.android.binaryeye.app.PERMISSION_LOCATION
import de.markusfisch.android.binaryeye.app.PERMISSION_WRITE
import de.markusfisch.android.binaryeye.app.applyLocale
import de.markusfisch.android.binaryeye.app.permissionGrantedCallback
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.view.initBars

abstract class AbstractBaseActivity : AppCompatActivity() {
	override fun attachBaseContext(base: Context?) {
		base?.applyLocale(prefs.customLocale)
		super.attachBaseContext(base)
	}

	protected fun setScreenContentView(@LayoutRes layoutId: Int) {
		setContentView(layoutId)
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
		return menu.isNotEmpty() || super.onCreateOptionsMenu(menu)
	}

	protected open fun onCreateOptionsMenu(
		menu: Menu,
		inflater: MenuInflater
	) = Unit
}
