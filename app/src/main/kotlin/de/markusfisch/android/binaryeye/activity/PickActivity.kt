package de.markusfisch.android.binaryeye.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.google.zxing.Result
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.colorSystemAndToolBars
import de.markusfisch.android.binaryeye.app.initSystemBars
import de.markusfisch.android.binaryeye.app.vibrate
import de.markusfisch.android.binaryeye.graphics.crop
import de.markusfisch.android.binaryeye.graphics.loadImageUri
import de.markusfisch.android.binaryeye.graphics.mapResult
import de.markusfisch.android.binaryeye.view.doOnApplyWindowInsets
import de.markusfisch.android.binaryeye.view.recordToolbarHeight
import de.markusfisch.android.binaryeye.widget.CropImageView
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.binaryeye.zxing.Zxing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickActivity : AppCompatActivity() {
	private val zxing = Zxing()

	private lateinit var vibrator: Vibrator
	private lateinit var cropImageView: CropImageView

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_pick)

		zxing.updateHints(true)
		vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

		initSystemBars(this)
		val toolbar = findViewById(R.id.toolbar) as Toolbar
		recordToolbarHeight(toolbar)
		setSupportActionBar(toolbar)

		supportFragmentManager.addOnBackStackChangedListener {
			colorSystemAndToolBars(this@PickActivity)
		}

		val bitmap = if (
			intent?.action == Intent.ACTION_SEND &&
			intent.type?.startsWith("image/") == true
		) {
			loadSentImage(intent)
		} else if (
			intent?.action == Intent.ACTION_VIEW &&
			intent.type?.startsWith("image/") == true
		) {
			loadImageToOpen(intent)
		} else {
			null
		}

		if (bitmap == null) {
			applicationContext.toast(R.string.error_no_content)
			finish()
			return
		}

		var result: Result? = null
		val scannedRect = Rect()
		fun scanWithinBounds() = crop(
			bitmap,
			cropImageView.normalizedRectInBounds,
			cropImageView.imageRotation
		)?.let {
			scannedRect.set(0, 0, it.width, it.height)
			CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
				result = zxing.decodePositiveNegative(it)
				result?.let {
					withContext(Dispatchers.Main) {
						if (!isFinishing) {
							vibrator.vibrate()
						}
						cropImageView.updateResultPoints(
							mapResult(
								scannedRect.width(),
								scannedRect.height(),
								0,
								cropImageView.getBoundsRect(),
								it
							)
						)
					}
				}
			}
		}

		cropImageView = findViewById(R.id.image) as CropImageView
		cropImageView.setImageBitmap(bitmap)
		cropImageView.onScan = {
			scanWithinBounds()
		}
		cropImageView.doOnApplyWindowInsets { v, insets ->
			(v as CropImageView).windowInsets.set(insets)
		}

		findViewById(R.id.scan).setOnClickListener {
			scanImage(result)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_pick, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.rotate -> {
				rotateClockwise()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun rotateClockwise() {
		cropImageView.imageRotation = cropImageView.imageRotation + 90 % 360
	}

	private fun loadSentImage(intent: Intent): Bitmap? {
		val uri = intent.getParcelableExtra<Parcelable>(
			Intent.EXTRA_STREAM
		) as? Uri ?: return null
		return loadImageUri(contentResolver, uri)
	}

	private fun loadImageToOpen(intent: Intent): Bitmap? {
		val uri = intent.data ?: return null
		return loadImageUri(contentResolver, uri)
	}

	private fun scanImage(result: Result?) {
		if (result != null) {
			showResult(this, result)
			finish()
			return
		}
		applicationContext.toast(R.string.no_barcode_found)
	}
}
