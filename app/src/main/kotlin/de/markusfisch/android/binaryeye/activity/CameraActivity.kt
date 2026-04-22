package de.markusfisch.android.binaryeye.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.adapter.setupFormatSpinner
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
import de.markusfisch.android.binaryeye.graphics.mapViewYToFrame
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
import de.markusfisch.android.binaryeye.zxingcpp.migrateBarcodeFormatName
import de.markusfisch.android.zxingcpp.ZxingCpp
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import de.markusfisch.android.zxingcpp.ZxingCpp.Binarizer
import de.markusfisch.android.zxingcpp.ZxingCpp.ReaderOptions
import de.markusfisch.android.zxingcpp.ZxingCpp.Result
import de.markusfisch.android.zxingcpp.ZxingCpp.TextMode
import java.io.FileInputStream
import java.util.Scanner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CameraActivity : AppCompatActivity() {
	private val analyzerExecutor: ExecutorService =
		Executors.newSingleThreadExecutor()
	private val readerOptions = ReaderOptions(
		tryHarder = prefs.tryHarder,
		tryRotate = prefs.autoRotate,
		tryInvert = true,
		tryDownscale = true,
		maxNumberOfSymbols = 1,
		textMode = TextMode.PLAIN
	)

	private lateinit var cameraView: PreviewView
	private lateinit var detectorView: DetectorView
	private lateinit var zoomBar: SeekBar
	private lateinit var flashFab: FloatingActionButton

	private var cameraProvider: ProcessCameraProvider? = null
	private var camera: Camera? = null
	private var preview: Preview? = null
	private var imageAnalysis: ImageAnalysis? = null
	private var currentProfile = prefs.profile
	private var shouldStoreSettings = true
	private var formatsToRead = setOf<BarcodeFormat>()
	private var decoding = true
	private var returnResult = false
	private var returnUrlTemplate: String? = null
	private var finishAfterShowingResult = false
	private var frontFacing = false
	private var bulkMode = prefs.bulkMode
	private var restrictFormat: String? = null
	private var searchTerm: Regex? = null
	private var ignoreNext: String? = null
	private var requestCameraPermission = true
	private var useLocalAverage = false

	@Volatile
	private var cameraViewSnapshot = CameraViewSnapshot()

	private data class CameraViewSnapshot(
		val viewWidth: Int = 0,
		val viewHeight: Int = 0,
		val roiLeft: Int = 0,
		val roiTop: Int = 0,
		val roiRight: Int = 0,
		val roiBottom: Int = 0,
		val horizontalCrosshairY: Float = 0f
	) {
		fun isValid() = viewWidth > 0 && viewHeight > 0

		fun toViewRoi() = Rect(roiLeft, roiTop, roiRight, roiBottom)
	}

	private data class CameraFrameMapping(
		val frameRoi: Rect,
		val matrix: Matrix,
		val horizontalCrosshairY: Float
	)

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
		super.onRequestPermissionsResult(
			requestCode,
			permissions,
			grantResults
		)
		when (requestCode) {
			PERMISSION_CAMERA -> {
				if (grantResults.isNotEmpty() &&
					grantResults[0] != PackageManager.PERMISSION_GRANTED
				) {
					toast(R.string.camera_error)
				} else if (grantResults.isNotEmpty()) {
					bindCameraUseCases()
				}
			}
		}
	}

	override fun onActivityResult(
		requestCode: Int,
		resultCode: Int,
		resultData: Intent?
	) {
		super.onActivityResult(requestCode, resultCode, resultData)
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

		cameraView = findViewById(R.id.camera_view)
		detectorView = findViewById(R.id.detector_view)
		zoomBar = findViewById(R.id.zoom)
		flashFab = findViewById(R.id.flash)

		initPreviewView()
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
		analyzerExecutor.shutdown()
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
			bindCameraUseCases()
		}
		requestCameraPermission = false
	}

	private fun Context.firstStart(): Boolean {
		val welcomeShownName = "welcome_shown"
		if (prefs.defaultPreferences.getBoolean(welcomeShownName, false)) {
			return false
		}
		prefs.defaultPreferences.edit {
			putBoolean(welcomeShownName, true)
		}
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
		zoomBar.progress = INITIAL_ZOOM
		currentProfile = prefs.profile
		recreate()
		return true
	}

	private fun loadPreferences() {
		detectorView.updateCropHandlePos()
		detectorView.invalidate()
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

	override fun onPause() {
		super.onPause()
		unbindCameraUseCases()
		if (shouldStoreSettings) {
			storeSettings()
		}
		shouldStoreSettings = true
	}

	private fun unbindCameraUseCases() {
		decoding = false
		imageAnalysis?.clearAnalyzer()
		imageAnalysis = null
		preview?.surfaceProvider = null
		preview = null
		cameraProvider?.unbindAll()
		camera = null
	}

	private fun storeSettings() {
		storeZoomBarSettings()
		detectorView.storeCropHandlePos()
	}

	override fun onRestoreInstanceState(savedState: Bundle) {
		super.onRestoreInstanceState(savedState)
		zoomBar.progress = savedState.getInt(ZOOM_LEVEL)
		frontFacing = savedState.getBoolean(FRONT_FACING)
		bulkMode = savedState.getBoolean(BULK_MODE)
		restrictFormat = savedState.getString(RESTRICT_FORMAT)
		searchTerm = savedState.getString(SEARCH_TERM)?.toRegex()
	}

	override fun onSaveInstanceState(outState: Bundle) {
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
				startActivity(Intent(this, HistoryActivity::class.java))
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
				askForSearchTerm()
				true
			}

			R.id.preferences -> {
				startActivity(Intent(this, PreferencesActivity::class.java))
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

			else -> {
				super.onOptionsItemSelected(item)
			}
		}
	}

	private fun createBarcode() {
		startActivity(EncodeActivity.newIntent<String>(this))
	}

	private fun switchCamera() {
		frontFacing = frontFacing xor true
		bindCameraUseCases()
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
	private fun askForSearchTerm() {
		val view = layoutInflater.inflate(R.layout.dialog_search_term, null)
		val editText = view.findViewById<EditText>(R.id.term)
		val formatView = view.findViewById<Spinner>(R.id.format)
		val values = setupFormatSpinner(formatView)
		searchTerm?.let {
			editText.setText(it.toString())
		}
		formatView.setSelection(
			values.indexOf(restrictFormat ?: "").coerceAtLeast(0)
		)
		AlertDialog.Builder(this)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val term = editText.text.toString().trim()
				restrictFormat = values[formatView.selectedItemPosition].ifEmpty {
					null
				}
				if (!term.isEmpty()) {
					searchTerm = term.toRegex()
				} else {
					searchTerm = null
				}
				ignoreNext = null
				updateHintsAndTitle()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
				// do not change anything on cancel
			}
			.show()
	}

	private fun openReadme() {
		val intent = Intent(
			Intent.ACTION_VIEW,
			getString(R.string.project_url).toUri()
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
					0 -> {
						null
					}

					else -> {
						profiles[which]
					}
				}
				if (currentProfile != newProfile) {
					switchProfile(newProfile)
				}
			}
			.show()
	}

	private fun switchProfile(name: String?) {
		storeSettings()
		prefs.load(this, name)
		loadPreferences()
		refreshIfProfileChanged()
	}

	private fun handleSendText(intent: Intent) {
		val text = intent.getStringExtra(Intent.EXTRA_TEXT)

		if (text?.isEmpty() == false) {
			startActivity(EncodeActivity.newIntent(this, text).apply {
				addFlags(
					Intent.FLAG_ACTIVITY_NO_HISTORY or
							Intent.FLAG_ACTIVITY_CLEAR_TASK or
							Intent.FLAG_ACTIVITY_NEW_TASK
				)
			})
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
						EncodeActivity.newIntent(this, scn.next()).apply {
							addFlags(
								Intent.FLAG_ACTIVITY_NO_HISTORY or
										Intent.FLAG_ACTIVITY_CLEAR_TASK or
										Intent.FLAG_ACTIVITY_NEW_TASK
							)
						}
					)
					finish()
				}
				file.close()
			}
		}
	}

	private fun initPreviewView() {
		cameraView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
			updateCameraViewSnapshot()
		}
		@Suppress("ClickableViewAccessibility")
		val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
		cameraView.setOnTouchListener(object : View.OnTouchListener {
			var downY = -1f
			var offset = -1f
			var progress = 0
			var hasSwiped = false

			override fun onTouch(v: View?, event: MotionEvent?): Boolean {
				event ?: return false
				val pos = event.y
				when (event.actionMasked) {
					MotionEvent.ACTION_DOWN -> {
						downY = pos
						offset = pos
						progress = zoomBar.progress
						hasSwiped = false
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
							if (abs(pos - downY) > touchSlop) {
								hasSwiped = true
							}
							return true
						}
					}

					MotionEvent.ACTION_UP -> {
						if (!hasSwiped) {
							camera?.cameraControl?.apply {
								cancelFocusAndMetering()
								startFocusAndMetering(
									FocusMeteringAction.Builder(
										cameraView.meteringPointFactory.createPoint(
											event.x,
											event.y
										)
									).build()
								)
							}
						}
						v?.performClick()
						return true
					}
				}
				return false
			}
		})
	}

	private fun initZoomBar() {
		zoomBar.max = 100
		zoomBar.progress = INITIAL_ZOOM
		restoreZoomBarSettings()
		zoomBar.setOnSeekBarChangeListener(object :
			SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(
				seekBar: SeekBar,
				progress: Int,
				fromUser: Boolean
			) {
				val maxRatio = camera?.cameraInfo?.zoomState?.value
					?.maxZoomRatio ?: return
				camera?.cameraControl?.setZoomRatio(
					1f + progress / 100f * (maxRatio - 1f)
				)
			}

			override fun onStartTrackingTouch(seekBar: SeekBar) {
			}

			override fun onStopTrackingTouch(seekBar: SeekBar) {
				storeZoomBarSettings()
			}
		})
	}

	private fun storeZoomBarSettings() {
		prefs.preferences.edit {
			putInt(ZOOM_LEVEL, zoomBar.progress)
		}
	}

	private fun restoreZoomBarSettings() {
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
			updateCameraViewSnapshot()
		}
		detectorView.setPaddingFromWindowInsets()
		detectorView.cropHandleName = "camera_crop_handle"
	}

	private fun bindCameraUseCases() {
		val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
		cameraProviderFuture.addListener({
			if (isDestroyed) {
				return@addListener
			}
			val provider = try {
				cameraProviderFuture.get()
			} catch (_: Exception) {
				toast(R.string.camera_error)
				return@addListener
			}
			cameraProvider = provider
			val resolutionSelector = ResolutionSelector.Builder()
				.setResolutionStrategy(
					ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
				)
				.setAspectRatioStrategy(
					AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
				)
				.setAllowedResolutionMode(
					ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
				)
				.build()
			val preview = Preview.Builder()
				.setResolutionSelector(resolutionSelector)
				.build()
			val imageAnalysis = ImageAnalysis.Builder()
				.setResolutionSelector(resolutionSelector)
				.setBackpressureStrategy(
					ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
				)
				.build()

			imageAnalysis.setAnalyzer(analyzerExecutor) { image ->
				analyzeImage(image)
			}

			val cameraSelector = CameraSelector.Builder()
				.requireLensFacing(
					if (frontFacing) {
						CameraSelector.LENS_FACING_FRONT
					} else {
						CameraSelector.LENS_FACING_BACK
					}
				)
				.build()

			try {
				provider.unbindAll()
				camera = provider.bindToLifecycle(
					this,
					cameraSelector,
					preview,
					imageAnalysis
				)
				preview.surfaceProvider = cameraView.surfaceProvider
				this.preview = preview
				this.imageAnalysis = imageAnalysis
				ignoreNext = null
				decoding = true
				useLocalAverage = false
				updateZoomState()
				updateFlashFab(camera?.cameraInfo?.hasFlashUnit() ?: false)
				updateCameraViewSnapshot()
			} catch (_: Exception) {
				camera = null
				toast(R.string.camera_error)
			}
		}, ContextCompat.getMainExecutor(this))
	}

	private fun analyzeImage(image: ImageProxy) {
		try {
			if (!decoding) {
				return
			}
			val frameMetrics = FrameMetrics(
				image.width,
				image.height,
				image.imageInfo.rotationDegrees
			)
			val frameMapping = getFrameMapping(frameMetrics) ?: return
			useLocalAverage = useLocalAverage xor true
			val yPlane = image.planes[0]
			ZxingCpp.readYBuffer(
				yPlane.buffer,
				yPlane.rowStride,
				frameMapping.frameRoi,
				frameMetrics.orientation,
				readerOptions.apply {
					binarizer = if (useLocalAverage) {
						Binarizer.LOCAL_AVERAGE
					} else {
						Binarizer.GLOBAL_HISTOGRAM
					}
					formats = formatsToRead
					tryHarder = prefs.tryHarder
					tryRotate = prefs.autoRotate
				}
			)?.firstOrNull()?.let { result ->
				handleDecodeResult(result, frameMapping)
			}
		} finally {
			image.close()
		}
	}

	private fun handleDecodeResult(
		result: Result,
		frameMapping: CameraFrameMapping
	) {
		val text = result.text
		if (text == ignoreNext) {
			return
		}
		val term = searchTerm
		if (term != null &&
			!text.matches(term) &&
			!text.contains(term)
		) {
			ignoreNext = text
			cameraView.post {
				errorFeedback()
				toast(R.string.does_not_match_search_term)
			}
			return
		}
		if (prefs.showCrosshairs &&
			!resultTouchesHorizontalCrosshair(result, frameMapping)
		) {
			return
		}
		decoding = false
		postResult(result, frameMapping)
	}

	private fun resultTouchesHorizontalCrosshair(
		result: Result,
		frameMapping: CameraFrameMapping
	): Boolean {
		val crosshairY = frameMapping.matrix.mapViewYToFrame(
			frameMapping.horizontalCrosshairY
		)
		val pos = result.position
		val minY = minOf(pos.topLeft.y, pos.topRight.y, pos.bottomLeft.y, pos.bottomRight.y)
		val maxY = maxOf(pos.topLeft.y, pos.topRight.y, pos.bottomLeft.y, pos.bottomRight.y)
		val centerY = (minY + maxY) / 2f
		val halfHeight = (maxY - minY) / 2f
		val halfWidth = abs(pos.topRight.x - pos.topLeft.x) / 2f
		return abs(centerY - crosshairY) <= maxOf(halfHeight, halfWidth / 3f)
	}

	private fun updateZoomState() {
		zoomBar.visibility = if (
			camera?.cameraInfo?.zoomState?.value != null
		) {
			View.VISIBLE
		} else {
			View.GONE
		}
	}

	private fun updateCameraViewSnapshot() {
		val viewRoi = if (
			detectorView.roi.width() < 1 ||
			detectorView.roi.height() < 1
		) {
			Rect(0, 0, cameraView.width, cameraView.height)
		} else {
			detectorView.roi
		}
		cameraViewSnapshot = CameraViewSnapshot(
			cameraView.width,
			cameraView.height,
			viewRoi.left,
			viewRoi.top,
			viewRoi.right,
			viewRoi.bottom,
			detectorView.getHorizontalCrosshairY()
		)
	}

	private fun getFrameMapping(
		frameMetrics: FrameMetrics
	): CameraFrameMapping? {
		val snapshot = cameraViewSnapshot
		if (!snapshot.isValid() || !frameMetrics.isValid()) {
			return null
		}
		val previewRect = Rect().apply {
			setCovered(
				snapshot.viewWidth,
				snapshot.viewHeight,
				frameMetrics
			)
		}
		val viewRoi = snapshot.toViewRoi()
		val frameRoi = Rect().apply {
			setFrameRoi(frameMetrics, previewRect, viewRoi)
		}
		if (frameRoi.width() < 1 || frameRoi.height() < 1) {
			return null
		}
		val matrix = Matrix().apply {
			setFrameToView(frameMetrics, previewRect, viewRoi)
		}
		return CameraFrameMapping(
			frameRoi,
			matrix,
			snapshot.horizontalCrosshairY
		)
	}

	private fun updateFlashFab(available: Boolean) {
		if (available) {
			flashFab.setImageResource(R.drawable.ic_action_flash)
			flashFab.setOnClickListener { toggleTorchMode() }
		} else {
			flashFab.setImageResource(R.drawable.ic_action_create)
			flashFab.setOnClickListener { createBarcode() }
		}
	}

	private fun toggleTorchMode() {
		val currentState = camera?.cameraInfo?.torchState?.value ?: return
		camera?.cameraControl?.enableTorch(
			currentState != TorchState.ON
		)
	}

	private fun Rect.setCovered(
		viewWidth: Int,
		viewHeight: Int,
		frameMetrics: FrameMetrics
	) {
		val frameWidth: Int
		val frameHeight: Int
		when (frameMetrics.orientation) {
			90, 270 -> {
				frameWidth = frameMetrics.height
				frameHeight = frameMetrics.width
			}

			else -> {
				frameWidth = frameMetrics.width
				frameHeight = frameMetrics.height
			}
		}
		if (frameWidth < 1 || frameHeight < 1) {
			set(0, 0, 0, 0)
			return
		}
		var coveredWidth = frameWidth
		var coveredHeight = frameHeight
		if (viewWidth.toLong() * coveredWidth <
			viewHeight.toLong() * coveredHeight
		) {
			coveredWidth = coveredWidth * viewHeight / coveredHeight
			coveredHeight = viewHeight
		} else {
			coveredHeight = coveredHeight * viewWidth / coveredWidth
			coveredWidth = viewWidth
		}
		val left = (viewWidth - coveredWidth) / 2
		val top = (viewHeight - coveredHeight) / 2
		set(
			left,
			top,
			left + coveredWidth,
			top + coveredHeight
		)
	}

	private fun postResult(
		result: Result,
		frameMapping: CameraFrameMapping
	) {
		cameraView.post {
			detectorView.update(
				frameMapping.matrix.mapPosition(
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
					setResult(RESULT_OK, getReturnIntent(result))
					finish()
				}

				returnUri != null -> {
					execShareIntent(
						Intent(Intent.ACTION_VIEW, returnUri)
					)
				}

				else -> {
					if (importProfile(result.text)) {
						return@post
					}
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
					Preferences.Companion.IgnoreDuplicates.Any -> {
						ignoreNext = result.text
					}

					else -> {
					}
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

	private fun importProfile(text: String): Boolean {
		val name = prefs.importProfile(this, text) ?: return false
		AlertDialog.Builder(this)
			.setMessage(getString(R.string.profile_import_set_default, name))
			.setPositiveButton(android.R.string.ok) { _, _ ->
				switchProfile(name)
			}
			.setNegativeButton(android.R.string.cancel, null)
			.setOnDismissListener {
				decoding = true
			}
			.show()
		return true
	}

	companion object {
		private const val PICK_FILE_RESULT_CODE = 1
		private const val INITIAL_ZOOM = 0
		private const val ZOOM_LEVEL = "zoom"
		private const val FRONT_FACING = "front_facing"
		private const val BULK_MODE = "bulk_mode"
		private const val RESTRICT_FORMAT = "restrict_format"
		private const val SEARCH_TERM = "search_term"
	}
}

fun Set<String>.toFormatSet(): Set<BarcodeFormat> = map {
	BarcodeFormat.valueOf(it.migrateBarcodeFormatName())
}.toSet()

fun Activity.showResult(
	result: Result,
	bulkMode: Boolean = false,
) {
	if (prefs.copyImmediately) {
		copyToClipboard(result.text)
	}
	val scan = result.toScan()
	if (prefs.useHistory &&
		!prefs.shouldIgnoreHistoryContent(scan.text, scan.format.name)
	) {
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

					else -> {
						R.string.bluetooth_send_success
					}
				}
			)
		}
	}
	if (runAutomatedActions(scan)) {
		return
	}
	if (!bulkMode) {
		startActivity(DecodeActivity.newIntent(this, scan))
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

private fun completeUrl(urlTemplate: String, result: Result) = (
		urlTemplate
			.replace("{RESULT}", result.text.urlEncode())
			.replace("{RESULT_BYTES}", result.rawBytes.toHexString())
			.replace("{FORMAT}", result.format.name.urlEncode())
			// And support {CODE} from the old ZXing app, too.
			.replace("{CODE}", result.text.urlEncode())
		).toUri()
