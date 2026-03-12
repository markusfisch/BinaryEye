package de.markusfisch.android.binaryeye.activity

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.applyLocale
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.fragment.PreferencesFragment
import de.markusfisch.android.binaryeye.view.initBars

class PreferencesActivity : AppCompatActivity() {
	override fun attachBaseContext(base: Context?) {
		base?.applyLocale(prefs.customLocale)
		super.attachBaseContext(base)
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_main)
		initBars()
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		if (state == null) {
			supportFragmentManager.beginTransaction()
				.replace(R.id.content_frame, PreferencesFragment())
				.commit()
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}
}
