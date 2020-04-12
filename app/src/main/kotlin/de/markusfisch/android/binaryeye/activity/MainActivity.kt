package de.markusfisch.android.binaryeye.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.colorSystemAndToolBars
import de.markusfisch.android.binaryeye.app.initSystemBars
import de.markusfisch.android.binaryeye.app.setFragment
import de.markusfisch.android.binaryeye.data.Scan
import de.markusfisch.android.binaryeye.fragment.DecodeFragment
import de.markusfisch.android.binaryeye.fragment.EncodeFragment
import de.markusfisch.android.binaryeye.fragment.HistoryFragment
import de.markusfisch.android.binaryeye.fragment.PreferencesFragment
import de.markusfisch.android.binaryeye.view.setupInsets

class MainActivity : AppCompatActivity() {
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
		val toolbar = findViewById(R.id.toolbar) as Toolbar
		setupInsets(
			findViewById(android.R.id.content),
			toolbar
		)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		supportFragmentManager.addOnBackStackChangedListener {
			colorSystemAndToolBars(this@MainActivity)
		}

		if (state == null) {
			setFragment(supportFragmentManager, getFragmentForIntent(intent))
		}
	}

	companion object {
		private const val PREFERENCES = "preferences"
		private const val HISTORY = "history"
		private const val ENCODE = "encode"
		private const val DECODED = "decoded"

		private fun getFragmentForIntent(intent: Intent?): Fragment {
			intent ?: return PreferencesFragment()
			return when {
				intent.hasExtra(PREFERENCES) -> PreferencesFragment()
				intent.hasExtra(HISTORY) -> HistoryFragment()
				intent.hasExtra(ENCODE) -> EncodeFragment.newInstance(
					intent.getStringExtra(ENCODE)!!
				)
				intent.hasExtra(DECODED) -> DecodeFragment.newInstance(
					intent.getParcelableExtra(DECODED)!!
				)
				else -> PreferencesFragment()
			}
		}

		fun getPreferencesIntent(context: Context): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(PREFERENCES, true)
			return intent
		}

		fun getHistoryIntent(context: Context): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(HISTORY, true)
			return intent
		}

		fun getEncodeIntent(
			context: Context,
			text: String? = "",
			isExternal: Boolean = false
		): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(ENCODE, text)
			if (isExternal) {
				val flagActivityClearTask =
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						Intent.FLAG_ACTIVITY_CLEAR_TASK
					} else 0
				intent.addFlags(
					Intent.FLAG_ACTIVITY_NO_HISTORY or
							flagActivityClearTask or
							Intent.FLAG_ACTIVITY_NEW_TASK
				)
			}
			return intent
		}

		fun getDecodeIntent(context: Context, scan: Scan): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(DECODED, scan)
			return intent
		}
	}
}
