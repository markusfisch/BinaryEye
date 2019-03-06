package de.markusfisch.android.binaryeye.activity

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.app.initSystemBars
import de.markusfisch.android.binaryeye.app.setFragment
import de.markusfisch.android.binaryeye.fragment.DecodeFragment
import de.markusfisch.android.binaryeye.fragment.EncodeFragment
import de.markusfisch.android.binaryeye.fragment.HistoryFragment
import de.markusfisch.android.binaryeye.R

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar

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

		setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		if (state == null) {
			var fragment: Fragment
			if (intent?.hasExtra(HISTORY) == true) {
				fragment = HistoryFragment()
			} else if (intent?.hasExtra(ENCODE) == true) {
				fragment = EncodeFragment.newInstance(
					intent.getStringExtra(ENCODE)
				)
			} else if (intent?.hasExtra(DECODE) == true) {
				fragment = DecodeFragment.newInstance(
					intent.getStringExtra(DECODE),
					intent.getSerializableExtra(
						DECODE_FORMAT
					) as BarcodeFormat
				)
			} else {
				fragment = DecodeFragment()
			}
			setFragment(supportFragmentManager, fragment)
		}
	}

	companion object {
		private const val HISTORY = "history"
		private const val ENCODE = "encode"
		private const val DECODE = "decode"
		private const val DECODE_FORMAT = "decode_format"

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
				intent.addFlags(
					android.content.Intent.FLAG_ACTIVITY_NO_HISTORY or
					android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or
					android.content.Intent.FLAG_ACTIVITY_NEW_TASK
				)
			}
			return intent
		}

		fun getDecodeIntent(
			context: Context,
			text: String,
			format: BarcodeFormat
		): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(DECODE, text)
			intent.putExtra(DECODE_FORMAT, format)
			return intent
		}
	}
}
