package de.markusfisch.android.binaryeye.activity

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.initSystemBars
import de.markusfisch.android.binaryeye.app.setFragment
import de.markusfisch.android.binaryeye.fragment.DecodeFragment
import de.markusfisch.android.binaryeye.fragment.EncodeFragment
import de.markusfisch.android.binaryeye.fragment.HistoryFragment

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
			setFragment(
				supportFragmentManager, when {
					intent?.hasExtra(HISTORY) == true -> HistoryFragment()
					intent?.hasExtra(ENCODE) == true -> EncodeFragment.newInstance(
						intent.getStringExtra(ENCODE)
					)
					intent?.hasExtra(DECODED_TEXT) == true -> DecodeFragment.newInstance(
						intent.getStringExtra(DECODED_TEXT),
						intent.getSerializableExtra(
							DECODED_FORMAT
						) as BarcodeFormat,
						intent.getByteArrayExtra(DECODED_RAW)
					)
					else -> DecodeFragment()
				}
			)
		}
	}

	companion object {
		private const val HISTORY = "history"
		private const val ENCODE = "encode"
		private const val DECODED_TEXT = "decoded_text"
		private const val DECODED_FORMAT = "decoded_format"
		private const val DECODED_RAW = "decoded_raw"

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
				val flagActivityClearTask = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
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

		fun getDecodeIntent(
			context: Context,
			text: String,
			format: BarcodeFormat,
			raw: ByteArray? = null
		): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(DECODED_TEXT, text)
			intent.putExtra(DECODED_FORMAT, format)
			if (raw != null) {
				intent.putExtra(DECODED_RAW, raw)
			}
			return intent
		}
	}
}
