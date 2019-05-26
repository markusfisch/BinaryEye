package de.markusfisch.android.binaryeye.activity

import com.google.zxing.Result
import com.google.zxing.ResultMetadataType

import de.markusfisch.android.cameraview.widget.CameraView

import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.hasCameraPermission
import de.markusfisch.android.binaryeye.app.initSystemBars
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.rs.Preprocessor
import de.markusfisch.android.binaryeye.zxing.Zxing
import de.markusfisch.android.binaryeye.R

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.os.Vibrator
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import java.io.IOException

class CameraActivity : AppCompatActivity() {
	private val zxing = Zxing()

	private lateinit var vibrator: Vibrator
	private lateinit var cameraView: CameraView
	private lateinit var zoomBar: SeekBar
	private lateinit var flashFab: View

	private var preprocessor: Preprocessor? = null
	private var invert = false
	private var flash = false
	private var returnResult = false
	private var frontFacing = false
	private var fallbackBuffer: IntArray? = null

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
		when (requestCode) {
			REQUEST_CAMERA -> if (grantResults.isNotEmpty() &&
				grantResults[0] != PackageManager.PERMISSION_GRANTED
			) {
				Toast.makeText(
					this,
					R.string.no_camera_no_fun,
					Toast.LENGTH_SHORT
				).show()
				finish()
			}
		}
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(R.layout.activity_camera)

		initSystemBars(this)
		setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)

		vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

		cameraView = findViewById(R.id.camera_view) as CameraView
		zoomBar = findViewById(R.id.zoom) as SeekBar
		flashFab = findViewById(R.id.flash)
		flashFab.setOnClickListener { toggleTorchMode() }

		initCameraView()
		initZoomBar()
		restoreZoom()

		if (intent?.action == Intent.ACTION_SEND) {
			if (intent.type == "text/plain") {
				handleSendText(intent)
			} else if (intent.type?.startsWith("image/") == true) {
				handleSendImage(intent)
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		preprocessor?.destroy()
		fallbackBuffer = null
		saveZoom()
	}

	override fun onResume() {
		super.onResume()
		System.gc()
		returnResult = "com.google.zxing.client.android.SCAN" == intent.action
		if (hasCameraPermission(this, REQUEST_CAMERA)) {
			openCamera()
		}
	}

	private fun openCamera() {
		cameraView.openAsync(
			CameraView.findCameraId(
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
	}

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putInt(ZOOM_MAX, zoomBar.max)
		outState.putInt(ZOOM_LEVEL, zoomBar.progress)
		super.onSaveInstanceState(outState)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_camera, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.create -> {
				startActivity(MainActivity.getEncodeIntent(this))
				true
			}
			R.id.history -> {
				startActivity(MainActivity.getHistoryIntent(this))
				true
			}
			R.id.switch_camera -> {
				switchCamera()
				true
			}
			R.id.info -> {
				openReadme()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun switchCamera() {
		closeCamera()
		frontFacing = frontFacing xor true
		openCamera()
	}

	private fun openReadme() {
		startActivity(
			Intent(
				Intent.ACTION_VIEW, Uri.parse(
					"https://github.com/markusfisch/BinaryEye/blob/master/README.md"
				)
			)
		)
	}

	private fun handleSendText(intent: Intent) {
		val text = intent.getStringExtra(Intent.EXTRA_TEXT)
		if (text?.isEmpty() == false) {
			startActivity(MainActivity.getEncodeIntent(this, text, true))
			finish()
		}
	}

	private fun handleSendImage(intent: Intent) {
		val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri ?: return
		val bitmap = try {
			MediaStore.Images.Media.getBitmap(contentResolver, uri)
		} catch (e: IOException) {
			null
		}
		if (bitmap == null) {
			Toast.makeText(
				this,
				R.string.error_no_content,
				Toast.LENGTH_SHORT
			).show()
			return
		}
		val result = zxing.decodePositiveNegative(downsizeIfBigger(bitmap, 1024))
		if (result != null) {
			showResult(result)
			finish()
			return
		}
		Toast.makeText(
			this,
			R.string.no_barcode_found,
			Toast.LENGTH_SHORT
		).show()
		finish()
	}

	private fun initCameraView() {
		cameraView.setUseOrientationListener(true)
		cameraView.setTapToFocus()
		cameraView.setOnCameraListener(object : CameraView.OnCameraListener {
			override fun onConfigureParameters(
				parameters: Camera.Parameters
			) {
				if (parameters.isZoomSupported) {
					val max = parameters.maxZoom
					if (zoomBar.max != max) {
						zoomBar.max = max
						zoomBar.progress = max / 10
						saveZoom()
					}
					parameters.zoom = zoomBar.progress
				} else {
					zoomBar.visibility = View.GONE
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
				Toast.makeText(
					this@CameraActivity,
					R.string.camera_error,
					Toast.LENGTH_SHORT
				).show()
				finish()
			}

			override fun onCameraReady(camera: Camera) {
				val frameWidth = cameraView.frameWidth
				val frameHeight = cameraView.frameHeight
				val frameOrientation = cameraView.frameOrientation
				var decoding = true
				camera.setPreviewCallback { frameData, _ ->
					if (decoding) {
						val result = decodeFrame(
							frameData,
							frameWidth,
							frameHeight,
							frameOrientation
						)
						result?.let {
							cameraView.post {
								vibrator.vibrate(100)
								showResult(result)
							}
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
				setZoom(progress)
			}

			override fun onStartTrackingTouch(seekBar: SeekBar) {}

			override fun onStopTrackingTouch(seekBar: SeekBar) {}
		})
	}

	private fun setZoom(zoom: Int) {
		val camera: Camera? = cameraView.camera
		camera?.let {
			try {
				val params = camera.parameters
				params.zoom = zoom
				camera.parameters = params
			} catch (e: RuntimeException) {
				// ignore; there's nothing we can do
			}
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

	private fun updateFlashFab(available: Boolean) {
		flashFab.visibility = if (available) {
			View.GONE
		} else {
			View.VISIBLE
		}
	}

	private fun toggleTorchMode() {
		val camera = cameraView.camera
		val parameters = camera?.parameters
		parameters?.flashMode = if (flash) {
			Camera.Parameters.FLASH_MODE_OFF
		} else {
			Camera.Parameters.FLASH_MODE_TORCH
		}
		parameters?.let {
			try {
				camera.parameters = parameters
				flash = flash xor true
			} catch (e: RuntimeException) {
				// ignore; there's nothing we can do
			}
		}
	}

	private fun decodeFrame(
		frameData: ByteArray?,
		frameWidth: Int,
		frameHeight: Int,
		frameOrientation: Int
	): Result? {
		frameData ?: return null
		invert = invert xor true
		val pp = preprocessor ?: Preprocessor(
			this,
			frameWidth,
			frameHeight,
			frameOrientation
		)
		pp.process(frameData)
		preprocessor = pp
		return zxing.decode(
			frameData,
			pp.outWidth,
			pp.outHeight,
			invert
		)
	}

	private fun showResult(result: Result) {
		val rawBytes = getRawBytes(result)

		if (prefs.useHistory) {
			GlobalScope.launch {
				db.insertScan(
					System.currentTimeMillis(),
					result.text,
					rawBytes,
					result.barcodeFormat.toString()
				)
			}
		}

		if (returnResult) {
			val resultIntent = Intent()
			resultIntent.putExtra("SCAN_RESULT", result.text)
			setResult(RESULT_OK, resultIntent)
			finish()
			return
		}

		startActivity(
			MainActivity.getDecodeIntent(
				this,
				result.text,
				result.barcodeFormat,
				rawBytes
			)
		)
	}

	companion object {
		private const val REQUEST_CAMERA = 1
		private const val ZOOM_MAX = "zoom_max"
		private const val ZOOM_LEVEL = "zoom_level"
	}
}

fun downsizeIfBigger(bitmap: Bitmap, max: Int): Bitmap {
	return if (Math.max(bitmap.width, bitmap.height) > max) {
		var width: Int = max
		var height: Int = max
		if (bitmap.width > bitmap.height) {
			height = Math.round(
				width.toFloat() / bitmap.width.toFloat() *
						bitmap.height.toFloat()
			)
		} else {
			width = Math.round(
				height.toFloat() / bitmap.height.toFloat() *
						bitmap.width.toFloat()
			)
		}
		Bitmap.createScaledBitmap(bitmap, width, height, true)
	} else {
		bitmap
	}
}

fun getRawBytes(result: Result): ByteArray? {
	val metadata = result.resultMetadata ?: return null
	val segments = metadata[ResultMetadataType.BYTE_SEGMENTS] ?: return null
	var bytes = ByteArray(0)
	@Suppress("UNCHECKED_CAST")
	for (seg in segments as Iterable<ByteArray>) {
		bytes += seg
	}
	return bytes
}
