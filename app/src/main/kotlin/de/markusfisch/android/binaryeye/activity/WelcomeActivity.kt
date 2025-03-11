package de.markusfisch.android.binaryeye.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.prefs

class WelcomeActivity : AppCompatActivity() {
	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_welcome)
		findViewById(R.id.expert).setOnClickListener {
			finish()
		}
		findViewById(R.id.simple).setOnClickListener {
			prefs.apply {
				showCropHandle = false
				zoomBySwiping = false
				beep = true
				copyImmediately = true
				openImmediately = true
				showMetaData = false
				showHexDump = false
				expandEscapeSequences = false
				brightenScreen = true
			}
			finish()
		}
	}
}
