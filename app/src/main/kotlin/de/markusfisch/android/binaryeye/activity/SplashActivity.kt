package de.markusfisch.android.binaryeye.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
	override fun onCreate(state: Bundle?) {
		super.onCreate(state)

		// It's important _not_ to inflate a layout file here
		// because that would happen after the app is fully
		// initialized what is too late.

		startActivity(Intent(applicationContext, CameraActivity::class.java))
		finish()
	}
}
