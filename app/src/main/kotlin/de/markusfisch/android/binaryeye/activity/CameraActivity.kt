package de.markusfisch.android.binaryeye.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.app.PERMISSION_CAMERA
import de.markusfisch.android.binaryeye.app.applyLocale
import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.hasBluetoothPermission
import de.markusfisch.android.binaryeye.app.hasCameraPermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.automation.runAutomatedActions
import de.markusfisch.android.binaryeye.bluetooth.sendBluetoothAsync
import de.markusfisch.android.binaryeye.content.copyToClipboard
import de.markusfisch.android.binaryeye.content.execShareIntent
import de.markusfisch.android.binaryeye.content.openUrl
import de.markusfisch.android.binaryeye.database.toScan
import de.markusfisch.android.binaryeye.graphics.FrameMetrics
import de.markusfisch.android.binaryeye.graphics.mapPosition
import de.markusfisch.android.binaryeye.graphics.setFrameRoi
import de.markusfisch.android.binaryeye.graphics.setFrameToView
import de.markusfisch.android.binaryeye.media.releaseToneGenerators
import de.markusfisch.android.binaryeye.net.isScanDeeplink
import de.markusfisch.android.binaryeye.net.sendAsync
import de.markusfisch.android.binaryeye.net.urlEncode
import de.markusfisch.android.binaryeye.preference.Preferences
import de.markusfisch.android.binaryeye.view.errorFeedback
import de.markusfisch.android.binaryeye.view.initBars
import de.markusfisch.android.binaryeye.view.scanFeedback
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.DetectorView
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.cameraview.widget.CameraView
import de.markusfisch.android.zxingcpp.ZxingCpp
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import de.markusfisch.android.zxingcpp.ZxingCpp.Binarizer
import de.markusfisch.android.zxingcpp.ZxingCpp.ReaderOptions
import de.markusfisch.android.zxingcpp.ZxingCpp.Result
import de.markusfisch.android.zxingcpp.ZxingCpp.TextMode
import java.io.FileInputStream
import java.util.Scanner
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CameraActivity : AppCompatActivity() {
	private val frameRoi = Rect()
	private val matrix = Matrix()

	private lateinit var cameraView: CameraView
	private lateinit var detectorView: DetectorView
	private lateinit var zoomBar: SeekBar
	private lateinit var flashFab: FloatingActionButton

	private var currentProfile = prefs.profile
	private var shouldStoreSettings = true
	private var formatsToRead = setOf<BarcodeFormat>()
	private var frameMetrics = FrameMetrics()
	private var decoding = true
	private var returnResult = false
	private var returnUrlTemplate: String? = null
	private var finishAfterShowingResult = false
	private var frontFacing = false
	private var bulkMode = prefs.bulkMode
	private var restrictFormat: String? = null
	private var searchTerm: Regex? = null
	private var ignoreNext: String? = null
	private var fallbackBuffer: IntArray? = null
	private var requestCameraPermission = true

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
		when (requestCode) {
			PERMISSION_CAMERA -> if (grantResults.isNotEmpty() &&
				grantResults[0] != PackageManager.PERMISSION_GRANTED
			) {
				toast(R.string.camera_error)
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
				if (resultCode == RESULT_OK && resultData != null) {
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

		initBars()

		cameraView = findViewById(R.id.camera_view) as CameraView
		detectorView = findViewById(R.id.detector_view) as DetectorView
		zoomBar = findViewById(R.id.zoom) as SeekBar
		flashFab = findViewById(R.id.flash) as FloatingActionButton

		initCameraView()
		initZoomBar()
		initDetectorView()

		if (intent?.action == Intent.ACTION_SEND &&
			intent.type?.startsWith("text/") == true
		) {
			handleSendText(intent)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		fallbackBuffer = null
		releaseToneGenerators()
	}

	override fun onResume() {
		super.onResume()
		if (firstStart()) {
			startActivity(Intent(this, WelcomeActivity::class.java))
			return
		}
		loadPreferences()
		if (refreshIfProfileChanged()) {
			return
		}
		System.gc()
		setReturnTarget(intent)
		// Avoid asking multiple times when the user has denied access
		// for this session. Otherwise ActivityCompat.requestPermissions()
		// will trigger onResume() again and again.
		if (hasCameraPermission(requestCameraPermission)) {
			openCamera()
		}
		requestCameraPermission = false
	}

	private fun Context.firstStart(): Boolean {
		val welcomeShownName = "welcome_shown"
		if (prefs.defaultPreferences.getBoolean(welcomeShownName, false)) {
			return false
		}
		prefs.defaultPreferences.edit().putBoolean(welcomeShownName, true).apply()
		val packageInfo = packageManager.getPackageInfo(packageName, 0)
		val installedSince = System.currentTimeMillis() -
				packageInfo.firstInstallTime
		return installedSince < 86400000
	}

	private fun refreshIfProfileChanged(): Boolean {
		if (currentProfile == prefs.profile) {
			return false
		}
		shouldStoreSettings = false
		zoomBar.max = 0 // Reset zoom when there are no preferences yet.
		currentProfile = prefs.profile
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			recreate()
		} else {
			finish()
			startActivity(Intent(this, CameraActivity::class.java))
		}
		return true
	}

	private fun loadPreferences() {
		detectorView.updateCropHandlePos()
		updateHintsAndTitle()
		if (prefs.bulkMode && bulkMode != prefs.bulkMode) {
			bulkMode = prefs.bulkMode
			invalidateOptionsMenu()
			ignoreNext = null
		}
	}

	private fun updateHintsAndTitle() {
		val restriction = restrictFormat
		formatsToRead = if (restriction != null) {
			setOf(restriction)
		} else {
			prefs.barcodeFormats
		}.toFormatSet()
		updateTitle()
	}

	private fun updateTitle() {
		if (searchTerm != null || restrictFormat != null) {
			title = getString(
				R.string.scan_for,
				listOfNotNull(
					searchTerm?.let { "\"$it\"" },
					restrictFormat?.prettifyFormatName(),
				).joinToString(" ")
			)
		} else {
			setTitle(R.string.scan_code)
		}
	}

	private fun setReturnTarget(intent: Intent?) {
		when {
			intent?.action == "com.google.zxing.client.android.SCAN" -> {
				returnResult = true
			}

			intent?.dataString?.let(::isScanDeeplink) == true -> {
				finishAfterShowingResult = true
				returnUrlTemplate = intent.data?.getQueryParameter("ret")
			}
		}
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
		if (shouldStoreSettings) {
			storeSettings();
		}
		shouldStoreSettings = true
	}

	private fun closeCamera() {
		cameraView.close()
	}

	private fun storeSettings() {
		storeZoomBarSettings()
		detectorView.storeCropHandlePos()
	}

	override fun onRestoreInstanceState(savedState: Bundle) {
		super.onRestoreInstanceState(savedState)
		zoomBar.max = savedState.getInt(ZOOM_MAX)
		zoomBar.progress = savedState.getInt(ZOOM_LEVEL)
		frontFacing = savedState.getBoolean(FRONT_FACING)
		bulkMode = savedState.getBoolean(BULK_MODE)
		restrictFormat = savedState.getString(RESTRICT_FORMAT)
		searchTerm = savedState.getString(SEARCH_TERM)?.toRegex()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putInt(ZOOM_MAX, zoomBar.max)
		outState.putInt(ZOOM_LEVEL, zoomBar.progress)
		outState.putBoolean(FRONT_FACING, frontFacing)
		outState.putBoolean(BULK_MODE, bulkMode)
		outState.putString(RESTRICT_FORMAT, restrictFormat)
		outState.putString(SEARCH_TERM, searchTerm?.toString())
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
		menu.findItem(R.id.profile).title = getString(
			R.string.current_profile,
			prefs.profile ?: getString(R.string.profile_default)
		)
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
						getString(R.string.pick_image_file)
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

			R.id.find_code -> {
				askForCode()
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

			R.id.profile -> {
				pickProfile()
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
				updateHintsAndTitle()
			}
			show()
		}
	}

	// Dialogs do not have a parent view.
	@SuppressLint("InflateParams")
	private fun askForCode() {
		val view = layoutInflater.inflate(R.layout.dialog_find_code, null)
		val editText = view.findViewById<EditText>(R.id.term)
		searchTerm?.let {
			editText.setText(it.toString())
		}
		AlertDialog.Builder(this)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val term = editText.text.toString().trim()
				if (!term.isEmpty()) {
					searchTerm = term.toRegex()
					ignoreNext = null
				}
				updateTitle()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
				searchTerm = null
				ignoreNext = null
				updateTitle()
			}
			.show()
	}

	private fun openReadme() {
		val intent = Intent(
			Intent.ACTION_VIEW,
			Uri.parse(getString(R.string.project_url))
		)
		execShareIntent(intent)
	}

	private fun pickProfile() {
		val profiles = arrayOf(
			getString(R.string.profile_default),
		) + prefs.profiles
		AlertDialog.Builder(this)
			.setTitle(R.string.profile)
			.setItems(profiles) { _, which ->
				val newProfile = when (which) {
					0 -> null
					else -> profiles[which]
				}
				if (currentProfile != newProfile) {
					storeSettings()
					prefs.load(this, newProfile)
					loadPreferences()
					refreshIfProfileChanged()
				}
			}
			.show()
	}

	private fun handleSendText(intent: Intent) {
		val text = intent.getStringExtra(Intent.EXTRA_TEXT)

		if (text?.isEmpty() == false) {
			startActivity(MainActivity.getEncodeIntent(this, text, true))
			finish()
			return
		}

		// Read text from file.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
			@Suppress("DEPRECATION")
			intent.getParcelableExtra(Intent.EXTRA_STREAM) as Uri?
		} else {
			intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
		}?.let { textUri ->
			val file = contentResolver.openFileDescriptor(textUri, "r")
			if (file != null) {
				val fs = FileInputStream(file.fileDescriptor)
				val scn = Scanner(fs).useDelimiter("\\A")
				if (scn.hasNext()) {
					startActivity(
						MainActivity.getEncodeIntent(
							this, scn.next(), true
						)
					)
					finish()
				}
				file.close()
			}
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
							val change = maxValue /
									v.height.toFloat() * 2f * dist
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
					restoreZoomBarSettings()
					val max = parameters.maxZoom
					if (zoomBar.max != max) {
						zoomBar.max = max
						zoomBar.progress = max / 10
						storeZoomBarSettings()
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
			}

			override fun onCameraReady(camera: Camera) {
				frameMetrics = FrameMetrics(
					cameraView.frameWidth,
					cameraView.frameHeight,
					cameraView.frameOrientation
				)
				updateFrameRoiAndMappingMatrix()
				ignoreNext = null
				decoding = true
				// These settings can't change while the camera is open.
				val options = ReaderOptions(
					tryHarder = prefs.tryHarder,
					tryRotate = prefs.autoRotate,
					tryInvert = true,
					tryDownscale = true,
					maxNumberOfSymbols = 1,
					textMode = TextMode.PLAIN
				)
				var useLocalAverage = false
				camera.setPreviewCallback { frameData, _ ->
					if (decoding) {
						useLocalAverage = useLocalAverage xor true
						ZxingCpp.readByteArray(
							frameData,
							frameMetrics.width,
							frameRoi.left, frameRoi.top,
							frameRoi.width(), frameRoi.height(),
							frameMetrics.orientation,
							options.apply {
								// By default, ZXing uses LOCAL_AVERAGE, but
								// this does not work well with inverted
								// barcodes on low-contrast backgrounds.
								binarizer = if (useLocalAverage) {
									Binarizer.LOCAL_AVERAGE
								} else {
									Binarizer.GLOBAL_HISTOGRAM
								}
								formats = formatsToRead
							}
						)?.let { results ->
							val result = results.first()
							val text = result.text
							if (text == ignoreNext) {
								return@let
							}
							val term = searchTerm
							if (term != null &&
								!text.matches(term) &&
								!text.contains(term)
							) {
								ignoreNext = text
								errorFeedback()
								toast(R.string.does_not_match_search_term)
								return@let
							}
							postResult(result)
							decoding = false
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
	}

	@Suppress("DEPRECATION")
	private fun Camera.setZoom(zoom: Int) {
		try {
			val params = parameters
			params.zoom = zoom
			parameters = params
		} catch (_: RuntimeException) {
			// Ignore. There's nothing we can do.
		}
	}

	private fun storeZoomBarSettings() {
		val editor = prefs.preferences.edit()
		editor.putInt(ZOOM_MAX, zoomBar.max)
		editor.putInt(ZOOM_LEVEL, zoomBar.progress)
		editor.apply()
	}

	private fun restoreZoomBarSettings() {
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
			updateFrameRoiAndMappingMatrix()
		}
		detectorView.setPaddingFromWindowInsets()
		detectorView.cropHandleName = "camera_crop_handle"
	}

	private fun updateFrameRoiAndMappingMatrix() {
		val viewRect = cameraView.previewRect
		if (viewRect.width() < 1 ||
			viewRect.height() < 1 ||
			!frameMetrics.isValid()
		) {
			return
		}
		val viewRoi = if (
			detectorView.roi.width() < 1 ||
			detectorView.roi.height() < 1
		) {
			viewRect
		} else {
			detectorView.roi
		}
		frameRoi.setFrameRoi(frameMetrics, viewRect, viewRoi)
		matrix.setFrameToView(frameMetrics, viewRect, viewRoi)
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

	private fun postResult(result: Result) {
		cameraView.post {
			detectorView.update(
				matrix.mapPosition(
					result.position,
					detectorView.coordinates
				)
			)
			scanFeedback()
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
					showResult(result, bulkMode)
					// If this app was invoked via a deep link but without
					// a return URI, we probably don't want to return to
					// the camera screen after scanning, but to the caller.
					if (finishAfterShowingResult) {
						finish()
					}
				}
			}
			if (bulkMode) {
				when (prefs.ignoreDuplicates()) {
					Preferences.Companion.IgnoreDuplicates.Consecutive,
					Preferences.Companion.IgnoreDuplicates.Any ->
						ignoreNext = result.text

					else -> Unit
				}
				if (prefs.showToastInBulkMode) {
					toast(result.text)
				}
				detectorView.postDelayed({
					decoding = true
				}, prefs.bulkModeDelay.toLong())
			}
		}
	}

	companion object {
		private const val PICK_FILE_RESULT_CODE = 1
		private const val ZOOM_MAX = "zoom_max"
		private const val ZOOM_LEVEL = "zoom_level"
		private const val FRONT_FACING = "front_facing"
		private const val BULK_MODE = "bulk_mode"
		private const val RESTRICT_FORMAT = "restrict_format"
		private const val SEARCH_TERM = "search_term"
	}
}

fun Set<String>.toFormatSet(): Set<BarcodeFormat> = map {
	BarcodeFormat.valueOf(it)
}.toSet()

fun Activity.showResult(
	result: Result,
	bulkMode: Boolean = false,
) {
	if (prefs.copyImmediately) {
		copyToClipboard(result.text)
	}
	val scan = result.toScan()
	if (prefs.useHistory) {
		scan.id = db.insertScan(scan)
	}
	if (prefs.sendScanActive && prefs.sendScanUrl.isNotEmpty()) {
		if (prefs.sendScanType == "4") {
			openUrl(
				prefs.sendScanUrl + scan.text.urlEncode()
			)
			return
		}
		scan.sendAsync(
			prefs.sendScanUrl,
			prefs.sendScanType
		) { code, body ->
			if (code == null || code < 200 || code > 299) {
				errorFeedback()
			}
			if (!body.isNullOrEmpty()) {
				toast(body)
			} else if (code == null || code > 299) {
				toast(R.string.background_request_failed)
			}
		}
	}
	if (prefs.sendScanBluetooth &&
		prefs.sendScanBluetoothHost.isNotEmpty() &&
		hasBluetoothPermission()
	) {
		scan.sendBluetoothAsync(
			prefs.sendScanBluetoothHost
		) { connected, sent ->
			toast(
				when {
					!connected -> {
						errorFeedback()
						R.string.bluetooth_connect_fail
					}

					!sent -> {
						errorFeedback()
						R.string.bluetooth_send_fail
					}

					else -> R.string.bluetooth_send_success
				}
			)
		}
	}
	if (runAutomatedActions(scan)) {
		return
	}
	if (!bulkMode) {
		startActivity(
			MainActivity.getDecodeIntent(this, scan)
		)
	}
}

private fun getReturnIntent(result: Result) = Intent().apply {
	putExtra("SCAN_RESULT", result.text)
	putExtra("SCAN_RESULT_FORMAT", result.format.toString())
	putExtra("SCAN_RESULT_ORIENTATION", result.orientation)
	putExtra("SCAN_RESULT_ERROR_CORRECTION_LEVEL", result.ecLevel)
	if (result.rawBytes.isNotEmpty()) {
		putExtra("SCAN_RESULT_BYTES", result.rawBytes)
	}
}

private fun completeUrl(urlTemplate: String, result: Result) = Uri.parse(
	urlTemplate
		.replace("{RESULT}", result.text.urlEncode())
		.replace("{RESULT_BYTES}", result.rawBytes.toHexString())
		.replace("{FORMAT}", result.format.name.urlEncode())
		// And support {CODE} from the old ZXing app, too.
		.replace("{CODE}", result.text.urlEncode())
)
