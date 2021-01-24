package de.markusfisch.android.binaryeye.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.support.v8.renderscript.RenderScript
import android.view.Menu
import android.view.MenuItem
import com.google.zxing.Result
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.*
import de.markusfisch.android.binaryeye.graphics.crop
import de.markusfisch.android.binaryeye.graphics.loadImageUri
import de.markusfisch.android.binaryeye.graphics.mapResult
import de.markusfisch.android.binaryeye.os.vibrate
import de.markusfisch.android.binaryeye.rs.fixTransparency
import de.markusfisch.android.binaryeye.view.colorSystemAndToolBars
import de.markusfisch.android.binaryeye.view.initSystemBars
import de.markusfisch.android.binaryeye.view.recordToolbarHeight
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.CropImageView
import de.markusfisch.android.binaryeye.widget.DetectorView
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.binaryeye.zxing.Zxing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickActivity : AppCompatActivity() {
	private val zxing = Zxing()

	private lateinit var rs: RenderScript
	private lateinit var vibrator: Vibrator
	private lateinit var cropImageView: CropImageView
	private lateinit var detectorView: DetectorView

	private var result: Result? = null

	override fun attachBaseContext(base: Context?) {
		base?.applyLocale(prefs.customLocale)
		super.attachBaseContext(base)
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_pick)

		// necessary to get the right translation after setting a custom locale
		setTitle(R.string.pick_code_to_scan)

		zxing.updateHints(true)
		rs = RenderScript.create(this)
		vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

		initSystemBars(this)
		val toolbar = findViewById(R.id.toolbar) as Toolbar
		recordToolbarHeight(toolbar)
		setSupportActionBar(toolbar)

		supportFragmentManager.addOnBackStackChangedListener {
			colorSystemAndToolBars(this@PickActivity)
		}

		val bitmap = fixTransparency(
			rs,
			getBitmapFromIntent()
		)

		if (bitmap == null) {
			applicationContext.toast(R.string.error_no_content)
			finish()
			return
		}

		cropImageView = findViewById(R.id.image) as CropImageView
		cropImageView.restrictTranslation = false
		cropImageView.setImageBitmap(bitmap)
		cropImageView.onScan = {
			scanWithinBounds(bitmap)
		}

		detectorView = findViewById(R.id.detector_view) as DetectorView
		detectorView.onRoiChanged = {
			scanWithinBounds(bitmap)
		}
		detectorView.setPaddingFromWindowInsets()

		findViewById(R.id.scan).setOnClickListener {
			showResult()
		}
	}

	private fun getBitmapFromIntent(): Bitmap? = if (
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

	private fun scanWithinBounds(bitmap: Bitmap) {
		val roi = detectorView.roi
		val rectInView = if (roi.width() < 1) {
			cropImageView.getBoundsRect()
		} else {
			roi
		}
		val imageRect = cropImageView.mappedRect
		val rectInImage = normalizeRoi(imageRect, rectInView)
		val cropped = crop(
			bitmap,
			rectInImage,
			cropImageView.imageRotation
		) ?: return
		CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
			result = zxing.decodePositiveNegative(cropped)
			result?.let {
				withContext(Dispatchers.Main) {
					if (!isFinishing) {
						vibrator.vibrate()
					}
					val rc = Rect()
					imageRect.round(rc)
					rectInView.intersect(rc)
					detectorView.mark(
						mapResult(
							cropped.width,
							cropped.height,
							0,
							rectInView,
							it
						)
					)
				}
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		rs.destroy()
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

	private fun showResult() {
		result?.let {
			showResult(this, it, vibrator)
			finish()
			return
		}
		applicationContext.toast(R.string.no_barcode_found)
	}
}

private fun normalizeRoi(imageRect: RectF, roi: Rect): RectF {
	val w = imageRect.width()
	val h = imageRect.height()
	return RectF(
		(roi.left - imageRect.left) / w,
		(roi.top - imageRect.top) / h,
		(roi.right - imageRect.left) / w,
		(roi.bottom - imageRect.top) / h
	)
}
