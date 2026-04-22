package de.markusfisch.android.binaryeye.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.prefs

class WelcomeActivity : AppCompatActivity() {
	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_welcome)
		findViewById<View>(R.id.expert).setOnClickListener {
			prefs.loadExpertProfile(this)
			finish()
		}
		findViewById<View>(R.id.simple).setOnClickListener {
			prefs.loadSimpleProfile(this)
			finish()
		}
	}
}
