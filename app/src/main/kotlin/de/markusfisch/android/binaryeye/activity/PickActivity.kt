package de.markusfisch.android.binaryeye.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
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
import de.markusfisch.android.binaryeye.app.*
import de.markusfisch.android.binaryeye.graphics.crop
import de.markusfisch.android.binaryeye.graphics.loadImageUri
import de.markusfisch.android.binaryeye.graphics.mapResult
import de.markusfisch.android.binaryeye.widget.CropImageView
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.binaryeye.zxing.Zxing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

class PickActivity : AppCompatActivity() {
	private val zxing = Zxing()

	private lateinit var vibrator: Vibrator
	private lateinit var cropImageView: CropImageView

	@FlowPreview
	@ExperimentalCoroutinesApi
	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_pick)

		zxing.updateHints(true)
		vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

		initSystemBars(this)
		val toolbar = findViewById(R.id.toolbar) as Toolbar
		setupInsets(findViewById(android.R.id.content), toolbar)
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

		val scannedRect = Rect()
		val scanWithinBounds: () -> Result? = {
			var result: Result? = null
			crop(
				bitmap,
				cropImageView.normalizedRectInBounds,
				cropImageView.imageRotation
			)?.also {
				scannedRect.set(0, 0, it.width, it.height)
				result = zxing.decodePositiveNegative(it)
			}
			result
		}

		cropImageView = findViewById(R.id.image) as CropImageView
		cropImageView.setImageBitmap(bitmap)
		cropImageView.onScan = {
			var points: List<Point>? = null
			scanWithinBounds()?.let {
				points = mapResult(
					scannedRect.width(),
					scannedRect.height(),
					cropImageView.getBoundsRect(),
					it
				)
				vibrator.vibrate()
			}
			points
		}
		setWindowInsetListener { insets ->
			cropImageView.windowInsets.set(insets)
		}

		findViewById(R.id.scan).setOnClickListener {
			scanImage(scanWithinBounds())
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

	@ExperimentalCoroutinesApi
	@FlowPreview
	private fun scanImage(result: Result?) {
		if (result != null) {
			showResult(this, result)
			finish()
			return
		}
		applicationContext.toast(R.string.no_barcode_found)
	}
}
