package de.markusfisch.android.binaryeye.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.zxing.Result
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.colorSystemAndToolBars
import de.markusfisch.android.binaryeye.app.initSystemBars
import de.markusfisch.android.binaryeye.app.setWindowInsetListener
import de.markusfisch.android.binaryeye.app.setupInsets
import de.markusfisch.android.binaryeye.graphics.crop
import de.markusfisch.android.binaryeye.graphics.loadImageUri
import de.markusfisch.android.binaryeye.widget.CropImageView
import de.markusfisch.android.binaryeye.zxing.Zxing
import kotlin.math.max
import kotlin.math.min

class PickActivity : AppCompatActivity() {
	private val zxing = Zxing()

	private lateinit var cropImageView: CropImageView

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_pick)

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
			Toast.makeText(
				applicationContext,
				R.string.error_no_content,
				Toast.LENGTH_SHORT
			).show()
			finish()
			return
		}

		val scannedRect = Rect()
		val scanBounds: () -> Result? = {
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
			val rect = Rect()
			scanBounds()?.also {
				rect.left = Int.MAX_VALUE
				rect.top = Int.MAX_VALUE
				for (rp in it.resultPoints) {
					val x = rp.x.toInt()
					val y = rp.y.toInt()
					rect.left = min(rect.left, x)
					rect.right = max(rect.right, x)
					rect.top = min(rect.top, y)
					rect.bottom = max(rect.bottom, y)
				}
				// map result points onto view
				val bounds = cropImageView.getBoundsRect()
				val fx = bounds.width() / scannedRect.width().toFloat()
				val fy = bounds.height() / scannedRect.height().toFloat()
				rect.set(
					(rect.left * fx).toInt(),
					(rect.top * fy).toInt(),
					(rect.right * fx).toInt(),
					(rect.bottom * fx).toInt()
				)
				rect.offset(bounds.left.toInt(), bounds.top.toInt())
			}
			rect
		}
		setWindowInsetListener { insets ->
			cropImageView.windowInsets.set(insets)
		}

		findViewById(R.id.scan).setOnClickListener {
			scanImage(scanBounds())
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
		Toast.makeText(
			applicationContext,
			R.string.no_barcode_found,
			Toast.LENGTH_SHORT
		).show()
	}
}
