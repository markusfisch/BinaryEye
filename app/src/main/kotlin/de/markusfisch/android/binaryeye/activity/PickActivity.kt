package de.markusfisch.android.binaryeye.activity

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.initSystemBars
import de.markusfisch.android.binaryeye.app.setSystemAndToolBarTransparency
import de.markusfisch.android.binaryeye.graphics.crop
import de.markusfisch.android.binaryeye.graphics.downsizeIfBigger
import de.markusfisch.android.binaryeye.widget.CropImageView
import de.markusfisch.android.binaryeye.zxing.Zxing
import java.io.IOException

class PickActivity : AppCompatActivity() {
	private val zxing = Zxing()

	private lateinit var cropImageView: CropImageView

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_pick)
		initSystemBars(this)

		setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		supportFragmentManager.addOnBackStackChangedListener {
			setSystemAndToolBarTransparency(this@PickActivity)
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

		cropImageView = findViewById(R.id.image) as CropImageView
		cropImageView.setImageBitmap(bitmap)

		findViewById(R.id.scan).setOnClickListener {
			val detail = crop(
				bitmap,
				cropImageView.normalizedRectInBounds,
				cropImageView.imageRotation
			)
			detail?.let {
				scanImage(detail)
			}
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
		return loadImageUri(uri)
	}

	private fun loadImageToOpen(intent: Intent): Bitmap? {
		val uri = intent.data ?: return null
		return loadImageUri(uri)
	}

	private fun loadImageUri(uri: Uri): Bitmap? = try {
		downsizeIfBigger(
			MediaStore.Images.Media.getBitmap(contentResolver, uri),
			1024
		)
	} catch (e: IOException) {
		null
	}

	private fun scanImage(bitmap: Bitmap) {
		val result = zxing.decodePositiveNegative(bitmap)
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
