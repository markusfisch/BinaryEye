package de.markusfisch.android.binaryeye.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Camera
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.support.v8.renderscript.RSRuntimeException
import android.support.v8.renderscript.RenderScript
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.app.*
import de.markusfisch.android.binaryeye.content.copyToClipboard
import de.markusfisch.android.binaryeye.content.execShareIntent
import de.markusfisch.android.binaryeye.content.openUrl
import de.markusfisch.android.binaryeye.database.toScan
import de.markusfisch.android.binaryeye.graphics.getFrameToViewMatrix
import de.markusfisch.android.binaryeye.graphics.map
import de.markusfisch.android.binaryeye.net.sendAsync
import de.markusfisch.android.binaryeye.os.error
import de.markusfisch.android.binaryeye.os.getVibrator
import de.markusfisch.android.binaryeye.os.vibrate
import de.markusfisch.android.binaryeye.rs.Preprocessor
import de.markusfisch.android.binaryeye.view.initSystemBars
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.DetectorView
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.binaryeye.zxing.Zxing
import de.markusfisch.android.cameraview.widget.CameraView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CameraActivity : AppCompatActivity() {
	private val zxing = Zxing { point ->
		point ?: return@Zxing
		val matrix = getMappingMatrix() ?: return@Zxing
		detectorView.post {
			detectorView.update(
				matrix.map(
					arrayOf(point),
					detectorView.coordinates
				)
			)
		}
	}

	private lateinit var rs: RenderScript
	private lateinit var vibrator: Vibrator
	private lateinit var cameraView: CameraView
	private lateinit var detectorView: DetectorView
	private lateinit var zoomBar: SeekBar
	private lateinit var flashFab: FloatingActionButton

	private var preprocessor: Preprocessor? = null
	private var nativeMappingMatrix: Matrix? = null
	private var rotatedMappingMatrix: Matrix? = null
	private var recreatePreprocessor = false
	private var rotate = false
	private var invert = false
	private var decoding = true
	private var returnResult = false
	private var returnUrlTemplate: String? = null
	private var finishAfterShowingResult = false
	private var frontFacing = false
	private var bulkMode = prefs.bulkMode
	private var restrictFormat: String? = null
	private var ignoreNext: String? = null
	private var fallbackBuffer: IntArray? = null

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
		when (requestCode) {
			PERMISSION_CAMERA -> if (grantResults.isNotEmpty() &&
				grantResults[0] != PackageManager.PERMISSION_GRANTED
			) {
				toast(R.string.no_camera_no_fun)
				finish()
			}
		}
	}

	override fun onActivityResult(
		requestCode: Int,
		resultCode: Int,
		resultData: Intent?
	) {
		when (requestCode) {
			PICK_FILE_RESULT_CODE -> {
				if (resultCode == Activity.RESULT_OK && resultData != null) {
					val pick = Intent(this, PickActivity::class.java)
					pick.action = Intent.ACTION_VIEW
					pick.setDataAndType(resultData.data, "image/*")
					startActivity(pick)
				}
			}
		}
	}

	override fun attachBaseContext(base: Context?) {
		base?.applyLocale(prefs.customLocale)
		super.attachBaseContext(base)
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_camera)

		// Necessary to get the right translation after setting a
		// custom locale.
		setTitle(R.string.scan_code)

		rs = RenderScript.create(this)
		vibrator = getVibrator()

		initSystemBars(this)
		setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)

		cameraView = findViewById(R.id.camera_view) as CameraView
		detectorView = findViewById(R.id.detector_view) as DetectorView
		zoomBar = findViewById(R.id.zoom) as SeekBar
		flashFab = findViewById(R.id.flash) as FloatingActionButton

		initCameraView()
		initZoomBar()
		initDetectorView()

		if (intent?.action == Intent.ACTION_SEND &&
			intent.type == "text/plain"
		) {
			handleSendText(intent)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		resetPreProcessor()
		fallbackBuffer = null
		saveZoom()
		detectorView.saveCropHandlePos()
		rs.destroy()
	}

	override fun onResume() {
		super.onResume()
		System.gc()
		updateHints()
		if (prefs.bulkMode && bulkMode != prefs.bulkMode) {
			bulkMode = prefs.bulkMode
			invalidateOptionsMenu()
			ignoreNext = null
		}
		setReturnTarget(intent)
		if (hasCameraPermission()) {
			openCamera()
		}
	}

	private fun updateHints() {
		val restriction = restrictFormat
		val formats = if (restriction != null) {
			title = getString(
				R.string.scan_format,
				prettifyFormatName(restriction)
			)
			setOf(restriction)
		} else {
			setTitle(R.string.scan_code)
			prefs.barcodeFormats
		}
		zxing.updateHints(prefs.tryHarder, formats)
	}

	private fun setReturnTarget(intent: Intent?) {
		when {
			intent?.action == "com.google.zxing.client.android.SCAN" -> {
				returnResult = true
			}
			intent?.dataString?.isReturnUrl() == true -> {
				finishAfterShowingResult = true
				returnUrlTemplate = intent.data?.getQueryParameter("ret")
			}
		}
	}

	private fun resetPreProcessor() {
		preprocessor?.destroy()
		preprocessor = null
	}

	private fun openCamera() {
		cameraView.openAsync(
			CameraView.findCameraId(
				@Suppress("DEPRECATION")
				if (frontFacing) {
					Camera.CameraInfo.CAMERA_FACING_FRONT
				} else {
					Camera.CameraInfo.CAMERA_FACING_BACK
				}
			)
		)
	}

	override fun onPause() {
		super.onPause()
		closeCamera()
	}

	private fun closeCamera() {
		cameraView.close()
	}

	override fun onRestoreInstanceState(savedState: Bundle) {
		super.onRestoreInstanceState(savedState)
		zoomBar.max = savedState.getInt(ZOOM_MAX)
		zoomBar.progress = savedState.getInt(ZOOM_LEVEL)
		frontFacing = savedState.getBoolean(FRONT_FACING)
		bulkMode = savedState.getBoolean(BULK_MODE)
		restrictFormat = savedState.getString(RESTRICT_FORMAT)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putInt(ZOOM_MAX, zoomBar.max)
		outState.putInt(ZOOM_LEVEL, zoomBar.progress)
		outState.putBoolean(FRONT_FACING, frontFacing)
		outState.putBoolean(BULK_MODE, bulkMode)
		outState.putString(RESTRICT_FORMAT, restrictFormat)
		super.onSaveInstanceState(outState)
	}

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		// Always give crop handle precedence over other controls
		// because it can easily overlap and would then be inaccessible.
		if (detectorView.onTouchEvent(ev)) {
			return true
		}
		return super.dispatchTouchEvent(ev)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_camera, menu)
		menu.findItem(R.id.bulk_mode).isChecked = bulkMode
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.create -> {
				createBarcode()
				true
			}
			R.id.history -> {
				startActivity(MainActivity.getHistoryIntent(this))
				true
			}
			R.id.pick_file -> {
				startActivityForResult(
					Intent.createChooser(
						Intent(Intent.ACTION_GET_CONTENT).apply {
							type = "image/*"
						},
						getString(R.string.pick_file)
					),
					PICK_FILE_RESULT_CODE
				)
				true
			}
			R.id.switch_camera -> {
				switchCamera()
				true
			}
			R.id.bulk_mode -> {
				bulkMode = bulkMode xor true
				item.isChecked = bulkMode
				ignoreNext = null
				true
			}
			R.id.restrict_format -> {
				showRestrictionDialog()
				true
			}
			R.id.preferences -> {
				startActivity(MainActivity.getPreferencesIntent(this))
				true
			}
			R.id.info -> {
				openReadme()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun createBarcode() {
		startActivity(MainActivity.getEncodeIntent(this))
	}

	private fun switchCamera() {
		closeCamera()
		frontFacing = frontFacing xor true
		openCamera()
	}

	private fun showRestrictionDialog() {
		val names = resources.getStringArray(
			R.array.barcode_formats_names
		).toMutableList()
		val formats = resources.getStringArray(
			R.array.barcode_formats_values
		).toMutableList()
		if (restrictFormat != null) {
			names.add(0, getString(R.string.remove_restriction))
			formats.add(0, null)
		}
		AlertDialog.Builder(this).apply {
			setTitle(R.string.restrict_format)
			setItems(names.toTypedArray()) { _, which ->
				restrictFormat = formats[which]
				updateHints()
			}
			show()
		}
	}

	private fun openReadme() {
		val intent = Intent(
			Intent.ACTION_VIEW,
			Uri.parse(getString(R.string.project_url))
		)
		execShareIntent(intent)
	}

	private fun handleSendText(intent: Intent) {
		val text = intent.getStringExtra(Intent.EXTRA_TEXT)
		if (text?.isEmpty() == false) {
			startActivity(MainActivity.getEncodeIntent(this, text, true))
			finish()
		}
	}

	private fun initCameraView() {
		cameraView.setUseOrientationListener(true)
		@Suppress("ClickableViewAccessibility")
		cameraView.setOnTouchListener(object : View.OnTouchListener {
			var focus = true
			var offset = -1f
			var progress = 0

			override fun onTouch(v: View?, event: MotionEvent?): Boolean {
				event ?: return false
				val pos = event.y
				when (event.actionMasked) {
					MotionEvent.ACTION_DOWN -> {
						offset = pos
						progress = zoomBar.progress
						return true
					}
					MotionEvent.ACTION_MOVE -> {
						if (prefs.zoomBySwiping) {
							v ?: return false
							val dist = offset - pos
							val maxValue = zoomBar.max
							val change = maxValue / v.height.toFloat() * 2f * dist
							zoomBar.progress = min(
								maxValue,
								max(progress + change.roundToInt(), 0)
							)
							return true
						}
					}
					MotionEvent.ACTION_UP -> {
						// Stop calling focusTo() as soon as it returns false
						// to avoid throwing and catching future exceptions.
						if (focus) {
							focus = cameraView.focusTo(v, event.x, event.y)
							if (focus) {
								v?.performClick()
								return true
							}
						}
					}
				}
				return false
			}
		})
		@Suppress("DEPRECATION")
		cameraView.setOnCameraListener(object : CameraView.OnCameraListener {
			override fun onConfigureParameters(
				parameters: Camera.Parameters
			) {
				zoomBar.visibility = if (parameters.isZoomSupported) {
					val max = parameters.maxZoom
					if (zoomBar.max != max) {
						zoomBar.max = max
						zoomBar.progress = max / 10
						saveZoom()
					}
					parameters.zoom = zoomBar.progress
					View.VISIBLE
				} else {
					View.GONE
				}
				val sceneModes = parameters.supportedSceneModes
				sceneModes?.let {
					for (mode in sceneModes) {
						if (mode == Camera.Parameters.SCENE_MODE_BARCODE) {
							parameters.sceneMode = mode
							break
						}
					}
				}
				CameraView.setAutoFocus(parameters)
				updateFlashFab(parameters.flashMode == null)
			}

			override fun onCameraError() {
				this@CameraActivity.toast(R.string.camera_error)
				finish()
			}

			override fun onCameraReady(camera: Camera) {
				// Reset preprocessor to make sure it always fits the current
				// frame orientation. Important for landscape to landscape
				// orientation changes.
				resetPreProcessor()
				val frameWidth = cameraView.frameWidth
				val frameHeight = cameraView.frameHeight
				val frameOrientation = cameraView.frameOrientation
				ignoreNext = null
				decoding = true
				camera.setPreviewCallback { frameData, _ ->
					if (decoding) {
						decodeFrame(
							frameData,
							frameWidth,
							frameHeight,
							frameOrientation
						)?.let { result ->
							if (result.text != ignoreNext) {
								postResult(result)
								decoding = false
							}
						}
					}
				}
			}

			override fun onPreviewStarted(camera: Camera) {
			}

			override fun onCameraStopping(camera: Camera) {
				camera.setPreviewCallback(null)
			}
		})
	}

	private fun initZoomBar() {
		zoomBar.setOnSeekBarChangeListener(object :
			SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(
				seekBar: SeekBar,
				progress: Int,
				fromUser: Boolean
			) {
				cameraView.camera?.setZoom(progress)
			}

			override fun onStartTrackingTouch(seekBar: SeekBar) {}

			override fun onStopTrackingTouch(seekBar: SeekBar) {}
		})
		restoreZoom()
	}

	@Suppress("DEPRECATION")
	private fun Camera.setZoom(zoom: Int) {
		try {
			val params = parameters
			params.zoom = zoom
			parameters = params
		} catch (e: RuntimeException) {
			// Ignore. There's nothing we can do.
		}
	}

	private fun saveZoom() {
		val editor = prefs.preferences.edit()
		editor.putInt(ZOOM_MAX, zoomBar.max)
		editor.putInt(ZOOM_LEVEL, zoomBar.progress)
		editor.apply()
	}

	private fun restoreZoom() {
		zoomBar.max = prefs.preferences.getInt(ZOOM_MAX, zoomBar.max)
		zoomBar.progress = prefs.preferences.getInt(
			ZOOM_LEVEL,
			zoomBar.progress
		)
	}

	private fun initDetectorView() {
		detectorView.onRoiChange = {
			decoding = false
		}
		detectorView.onRoiChanged = {
			decoding = true
			recreatePreprocessor = true
		}
		detectorView.setPaddingFromWindowInsets()
		detectorView.restoreCropHandlePos()
	}

	private fun updateFlashFab(unavailable: Boolean) {
		if (unavailable) {
			flashFab.setImageResource(R.drawable.ic_action_create)
			flashFab.setOnClickListener { createBarcode() }
		} else {
			flashFab.setImageResource(R.drawable.ic_action_flash)
			flashFab.setOnClickListener { toggleTorchMode() }
		}
	}

	@Suppress("DEPRECATION")
	private fun toggleTorchMode() {
		val camera = cameraView.camera ?: return
		val parameters = camera.parameters ?: return
		parameters.flashMode = if (
			parameters.flashMode != Camera.Parameters.FLASH_MODE_OFF
		) {
			Camera.Parameters.FLASH_MODE_OFF
		} else {
			Camera.Parameters.FLASH_MODE_TORCH
		}
		try {
			camera.parameters = parameters
		} catch (e: RuntimeException) {
			toast(e.message ?: getString(R.string.error_flash))
		}
	}

	private fun decodeFrame(
		frameData: ByteArray?,
		frameWidth: Int,
		frameHeight: Int,
		frameOrientation: Int
	): Result? {
		frameData ?: return null
		rotate = if (prefs.autoRotate) {
			rotate xor true
		} else {
			invert = invert xor true
			isPortrait(frameOrientation)
		}
		return try {
			if (recreatePreprocessor) {
				resetPreProcessor()
				recreatePreprocessor = false
			}
			val pp = preprocessor ?: createPreprocessorAndMapping(
				frameWidth,
				frameHeight,
				frameOrientation
			)
			val w: Int
			val h: Int
			if (rotate) {
				pp.resizeAndRotate(frameData)
				w = pp.outHeight
				h = pp.outWidth
			} else {
				invert = invert xor true
				pp.resizeOnly(frameData)
				w = pp.outWidth
				h = pp.outHeight
			}
			preprocessor = pp
			zxing.decode(frameData, w, h, invert)
		} catch (e: RSRuntimeException) {
			prefs.forceCompat = prefs.forceCompat xor true
			// Now the only option is to restart the app because
			// RenderScript.forceCompat() needs to be called before
			// RenderScript is initialized.
			restartApp()
			null
		}
	}

	private fun createPreprocessorAndMapping(
		frameWidth: Int,
		frameHeight: Int,
		frameOrientation: Int
	): Preprocessor {
		val frameRoi = calculateFrameRect(
			frameWidth,
			frameHeight,
			frameOrientation
		)
		val pp = Preprocessor(
			rs,
			frameWidth,
			frameHeight,
			frameRoi
		)
		val viewRect = if (frameRoi != null) {
			detectorView.roi
		} else {
			cameraView.previewRect
		}
		nativeMappingMatrix = getFrameToViewMatrix(
			pp.outWidth,
			pp.outHeight,
			frameOrientation,
			viewRect
		)
		rotatedMappingMatrix = getFrameToViewMatrix(
			pp.outHeight,
			pp.outWidth,
			(frameOrientation - 90 + 360) % 360,
			viewRect
		)
		return pp
	}

	private fun calculateFrameRect(
		frameWidth: Int,
		frameHeight: Int,
		frameOrientation: Int
	): Rect? {
		val viewRoi = detectorView.roi
		return if (
			viewRoi.width() == 0 ||
			viewRoi.height() == 0
		) {
			null
		} else {
			// Map ROI in detectorView to cameraView.
			val previewRect = cameraView.previewRect
			// previewRect may be larger than the screen (and thus as the
			// detectorView) in which case its left and/or top coordinate
			// will be negative which must then be clamped to the
			// screen/DetectorView.
			val previewLeft = max(0, previewRect.left)
			val previewTop = max(0, previewRect.top)
			val previewRoi = Rect(
				previewLeft + viewRoi.left,
				previewTop + viewRoi.top,
				previewLeft + viewRoi.right,
				previewTop + viewRoi.bottom
			)
			val previewRectWidth = previewRect.width().toFloat()
			val previewRectHeight = previewRect.height().toFloat()
			val normalizedRoi = RectF(
				previewRoi.left.toFloat() / previewRectWidth,
				previewRoi.top.toFloat() / previewRectHeight,
				previewRoi.right.toFloat() / previewRectWidth,
				previewRoi.bottom.toFloat() / previewRectHeight
			)
			// Since the ROI is always centered and symmetric, we don't
			// need to distinguish between 0 and 180 or 90 and 270 degree.
			val errectedRoi = if (isPortrait(frameOrientation)) {
				RectF(
					normalizedRoi.top,
					normalizedRoi.left,
					normalizedRoi.bottom,
					normalizedRoi.right
				)
			} else {
				normalizedRoi
			}
			Rect(
				(errectedRoi.left * frameWidth.toFloat()).roundToInt(),
				(errectedRoi.top * frameHeight.toFloat()).roundToInt(),
				(errectedRoi.right * frameWidth.toFloat()).roundToInt(),
				(errectedRoi.bottom * frameHeight.toFloat()).roundToInt()
			)
		}
	}

	private fun postResult(result: Result) {
		// Get matrix for the current rotate value.
		val matrix = getMappingMatrix()
		val resultPoints = result.resultPoints
		if (matrix == null ||
			resultPoints == null ||
			resultPoints.isEmpty()
		) {
			return
		}
		cameraView.post {
			detectorView.update(
				matrix.map(
					resultPoints,
					detectorView.coordinates
				)
			)
			vibrator.vibrate()
			val returnUri = returnUrlTemplate?.let {
				try {
					completeUrl(it, result)
				} catch (e: Exception) {
					e.message?.let { message ->
						toast(message)
					}
					null
				}
			}
			when {
				returnResult -> {
					setResult(Activity.RESULT_OK, getReturnIntent(result))
					finish()
				}
				returnUri != null -> execShareIntent(
					Intent(Intent.ACTION_VIEW, returnUri)
				)
				else -> {
					showResult(
						this@CameraActivity,
						result,
						vibrator,
						bulkMode
					)
					// If this app was invoked via a deep link but without
					// a return URI, we probably don't want to return to
					// the camera screen after scanning, but to the caller.
					if (finishAfterShowingResult) {
						finish()
					}
				}
			}
			if (bulkMode) {
				ignoreNext = result.text
				if (prefs.showToastInBulkMode) {
					toast(result.text)
				}
				detectorView.postDelayed({
					decoding = true
				}, prefs.bulkModeDelay.toLong())
			}
		}
	}

	private fun getMappingMatrix() = if (rotate) {
		rotatedMappingMatrix
	} else {
		nativeMappingMatrix
	}

	companion object {
		private const val PICK_FILE_RESULT_CODE = 1
		private const val ZOOM_MAX = "zoom_max"
		private const val ZOOM_LEVEL = "zoom_level"
		private const val FRONT_FACING = "front_facing"
		private const val BULK_MODE = "bulk_mode"
		private const val RESTRICT_FORMAT = "restrict_format"
	}
}

fun showResult(
	activity: Activity,
	result: Result,
	vibrator: Vibrator,
	bulkMode: Boolean = false,
) {
	if (prefs.copyImmediately) {
		activity.copyToClipboard(result.text)
	}
	val scan = result.toScan()
	if (prefs.useHistory) {
		scan.id = db.insertScan(scan)
	}
	if (prefs.sendScanActive && prefs.sendScanUrl.isNotEmpty()) {
		if (prefs.sendScanType == "4") {
			activity.openUrl(
				prefs.sendScanUrl + scan.content.urlEncode()
			)
			return
		}
		scan.sendAsync(
			prefs.sendScanUrl,
			prefs.sendScanType
		) { code, body ->
			if (code == null || code < 200 || code > 299) {
				vibrator.error()
				if (!activity.isSilent()) {
					beepBeepBeep()
				}
			}
			if (body != null && body.isNotEmpty()) {
				activity.toast(body)
			} else if (code == null || code > 299) {
				activity.toast(R.string.background_request_failed)
			}
		}
	}
	if (!bulkMode) {
		activity.startActivity(
			MainActivity.getDecodeIntent(activity, scan)
		)
	}
}

fun getReturnIntent(result: Result): Intent {
	val intent = Intent()
	intent.putExtra("SCAN_RESULT", result.text)
	intent.putExtra(
		"SCAN_RESULT_FORMAT",
		result.barcodeFormat.toString()
	)
	if (result.rawBytes?.isNotEmpty() == true) {
		intent.putExtra("SCAN_RESULT_BYTES", result.rawBytes)
	}
	result.resultMetadata?.let { metadata ->
		metadata[ResultMetadataType.ORIENTATION]?.let {
			intent.putExtra(
				"SCAN_RESULT_ORIENTATION",
				it.toString()
			)
		}
		metadata[ResultMetadataType.ERROR_CORRECTION_LEVEL]?.let {
			intent.putExtra(
				"SCAN_RESULT_ERROR_CORRECTION_LEVEL",
				it.toString()
			)
		}
		metadata[ResultMetadataType.UPC_EAN_EXTENSION]?.let {
			intent.putExtra(
				"SCAN_RESULT_UPC_EAN_EXTENSION",
				it.toString()
			)
		}
		metadata[ResultMetadataType.BYTE_SEGMENTS]?.let {
			var i = 0
			@Suppress("UNCHECKED_CAST")
			for (seg in it as Iterable<ByteArray>) {
				intent.putExtra("SCAN_RESULT_BYTE_SEGMENTS_$i", seg)
				++i
			}
		}
	}
	return intent
}

private fun String.isReturnUrl() = listOf(
	"binaryeye://scan",
	"http://markusfisch.de/BinaryEye",
	"https://markusfisch.de/BinaryEye"
).firstOrNull { startsWith(it) } != null

private fun completeUrl(urlTemplate: String, result: Result) = Uri.parse(
	urlTemplate
		.replace("{RESULT}", result.text.urlEncode())
		.replace("{RESULT_BYTES}", result.rawBytes?.toHexString() ?: "")
		.replace(
			"{FORMAT}", result.barcodeFormat.toString().urlEncode()
		)
		.replace(
			"{META}", result.resultMetadata?.toString()?.urlEncode() ?: ""
		)
		// And support {CODE} from the old ZXing app, too.
		.replace("{CODE}", result.text.urlEncode())
)

private fun Context.isSilent(): Boolean {
	val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
	return when (am.ringerMode) {
		AudioManager.RINGER_MODE_SILENT,
		AudioManager.RINGER_MODE_VIBRATE -> true
		else -> false
	}
}

private fun beepBeepBeep() {
	ToneGenerator(AudioManager.STREAM_ALARM, 100).startTone(
		ToneGenerator.TONE_CDMA_CONFIRM,
		1000
	)
}

private fun isPortrait(
	orientation: Int
) = orientation == 90 || orientation == 270
