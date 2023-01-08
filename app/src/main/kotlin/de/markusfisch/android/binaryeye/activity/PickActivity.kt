package de.markusfisch.android.binaryeye.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.applyLocale
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.graphics.crop
import de.markusfisch.android.binaryeye.graphics.fixTransparency
import de.markusfisch.android.binaryeye.graphics.loadImageUri
import de.markusfisch.android.binaryeye.graphics.mapPosition
import de.markusfisch.android.binaryeye.media.releaseToneGenerators
import de.markusfisch.android.binaryeye.view.*
import de.markusfisch.android.binaryeye.widget.CropImageView
import de.markusfisch.android.binaryeye.widget.DetectorView
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.zxingcpp.ZxingCpp
import de.markusfisch.android.zxingcpp.ZxingCpp.Result
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PickActivity : AppCompatActivity() {
	private val matrix = Matrix()
	private val parentJob = Job()
	private val scope = CoroutineScope(Dispatchers.IO + parentJob)

	private lateinit var cropImageView: CropImageView
	private lateinit var detectorView: DetectorView
	private lateinit var freeRotationItem: MenuItem

	private var result: Result? = null

	override fun attachBaseContext(base: Context?) {
		base?.applyLocale(prefs.customLocale)
		super.attachBaseContext(base)
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_pick)

		// Necessary to get the right translation after setting a custom
		// locale.
		setTitle(R.string.pick_code_to_scan)

		initSystemBars(this)
		val toolbar = findViewById(R.id.toolbar) as Toolbar
		recordToolbarHeight(toolbar)
		setSupportActionBar(toolbar)

		supportFragmentManager.addOnBackStackChangedListener {
			colorSystemAndToolBars(this@PickActivity)
		}

		val bitmap = getBitmapFromIntent()?.fixTransparency()
		if (bitmap == null) {
			applicationContext.toast(R.string.error_no_content)
			finish()
			return
		}

		cropImageView = findViewById(R.id.image) as CropImageView
		cropImageView.restrictTranslation = false
		cropImageView.freeRotation = prefs.freeRotation
		cropImageView.setImageBitmap(bitmap)
		cropImageView.onScan = {
			scanWithinBounds(bitmap)
		}

		detectorView = findViewById(R.id.detector_view) as DetectorView
		detectorView.onRoiChanged = {
			scanWithinBounds(bitmap)
		}
		detectorView.setPaddingFromWindowInsets()
		detectorView.restoreCropHandlePos()

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
		val viewRoi = if (detectorView.roi.width() < 1) {
			cropImageView.getBoundsRect()
		} else {
			detectorView.roi
		}
		val mappedRect = cropImageView.mappedRect
		val cropped = bitmap.crop(
			getNormalizedRoi(mappedRect, viewRoi),
			cropImageView.imageRotation,
			cropImageView.pivotX,
			cropImageView.pivotY
		) ?: return
		val croppedInView = Rect(
			max(viewRoi.left, mappedRect.left.roundToInt()),
			max(viewRoi.top, mappedRect.top.roundToInt()),
			min(viewRoi.right, mappedRect.right.roundToInt()),
			min(viewRoi.bottom, mappedRect.bottom.roundToInt())
		)
		matrix.apply {
			setScale(
				croppedInView.width().toFloat() / cropped.width,
				croppedInView.height().toFloat() / cropped.height
			)
			postTranslate(
				croppedInView.left.toFloat(),
				croppedInView.top.toFloat()
			)
		}
		scope.launch {
			ZxingCpp.readBitmap(
				cropped,
				0, 0,
				cropped.width, cropped.height,
				0,
				prefs.barcodeFormats.joinToString(),
				tryHarder = true,
				tryRotate = true,
				tryInvert = true,
				tryDownscale = true
			)?.let {
				withContext(Dispatchers.Main) {
					if (isFinishing) {
						return@withContext
					}
					result = it
					scanFeedback()
					detectorView.update(
						matrix.mapPosition(
							it.position,
							detectorView.coordinates
						)
					)
				}
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		detectorView.saveCropHandlePos()
		parentJob.cancel()
		releaseToneGenerators()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_pick, menu)
		freeRotationItem = menu.findItem(R.id.toggle_free).apply {
			updateFreeRotationIcon()
		}
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.rotate -> {
				rotateClockwise()
				true
			}
			R.id.toggle_free -> {
				prefs.freeRotation = prefs.freeRotation xor true
				cropImageView.freeRotation = prefs.freeRotation
				freeRotationItem.updateFreeRotationIcon()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun MenuItem.updateFreeRotationIcon() {
		setIcon(
			if (prefs.freeRotation) {
				R.drawable.ic_action_rotation_unlocked
			} else {
				R.drawable.ic_action_rotation_locked
			}
		)
	}

	private fun rotateClockwise() {
		cropImageView.imageRotation = cropImageView.imageRotation + 90 % 360
	}

	private fun loadSentImage(intent: Intent): Bitmap? {
		val uri = intent.getParcelableExtra<Parcelable>(
			Intent.EXTRA_STREAM
		) as? Uri ?: return null
		return contentResolver.loadImageUri(uri)
	}

	private fun loadImageToOpen(intent: Intent): Bitmap? {
		val uri = intent.data ?: return null
		return contentResolver.loadImageUri(uri)
	}

	private fun showResult() {
		val r = result
		if (r != null) {
			showResult(this, r)
			finish()
		} else {
			applicationContext.toast(R.string.no_barcode_found)
		}
	}
}

private fun getNormalizedRoi(imageRect: RectF, roi: Rect): RectF {
	val w = imageRect.width()
	val h = imageRect.height()
	return RectF(
		(roi.left - imageRect.left) / w,
		(roi.top - imageRect.top) / h,
		(roi.right - imageRect.left) / w,
		(roi.bottom - imageRect.top) / h
	)
}
