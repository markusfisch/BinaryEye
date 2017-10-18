package de.markusfisch.android.binaryeye.fragment

import com.google.zxing.Result
import com.google.zxing.ResultPoint

import de.markusfisch.android.cameraview.widget.CameraView

import de.markusfisch.android.binaryeye.activity.MainActivity
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.rs.YuvToGray
import de.markusfisch.android.binaryeye.widget.LockOnView
import de.markusfisch.android.binaryeye.zxing.Zxing
import de.markusfisch.android.binaryeye.R

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast

class CameraFragment : Fragment() {
	private val zxing = Zxing()
	private val decodingRunnable = Runnable {
		while (decoding) {
			val result = decodeFrame()
			if (result != null) {
				cameraView.post { found(result) }
				decoding = false
				break
			}
		}
	}

	private lateinit var vibrator: Vibrator
	private lateinit var cameraView: CameraView
	private lateinit var zoomBar: SeekBar
	private lateinit var lockOnView: LockOnView
	private lateinit var yuvToGray: YuvToGray

	private var decoding = false
	private var decodingThread: Thread? = null
	private var frameData: ByteArray? = null
	private var frameWidth: Int = 0
	private var frameHeight: Int = 0
	private var frameOrientation: Int = 0
	private var odd = false
	private var flash = false

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
			inflater: LayoutInflater,
			container: ViewGroup?,
			state: Bundle?): View? {
		activity.setTitle(R.string.scan_code)
		vibrator = activity.getSystemService(
				Context.VIBRATOR_SERVICE) as Vibrator

		val view = inflater.inflate(
				R.layout.fragment_camera,
				container,
				false)

		zoomBar = view.findViewById<SeekBar>(R.id.zoom)
		zoomBar.setOnSeekBarChangeListener(object :
				SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar, progress: Int,
					fromUser: Boolean) {
				setZoom(progress)
			}

			override fun onStartTrackingTouch(seekBar: SeekBar) {}

			override fun onStopTrackingTouch(seekBar: SeekBar) {}
		})

		view.findViewById<View>(R.id.create).setOnClickListener { _ ->
			addFragment(fragmentManager, EncodeFragment())
		}

		yuvToGray = YuvToGray(context)

		cameraView = (activity as MainActivity).cameraView
		cameraView.setOnCameraListener(object : CameraView.OnCameraListener {
			override fun onConfigureParameters(
					parameters: Camera.Parameters) {
				if (parameters.isZoomSupported()) {
					val max = parameters.getMaxZoom()
					zoomBar.max = max
					zoomBar.progress = max / 2
					parameters.setZoom(zoomBar.progress)
				} else {
					zoomBar.visibility = View.GONE
				}
				for (mode in parameters.getSupportedSceneModes()) {
					if (mode.equals(Camera.Parameters.SCENE_MODE_BARCODE)) {
						parameters.setSceneMode(mode)
						break
					}
				}
				CameraView.setAutoFocus(parameters)
			}

			override fun onCameraError(camera: Camera) {
				Toast.makeText(activity, R.string.camera_error,
						Toast.LENGTH_SHORT).show()
				activity.finish()
			}

			override fun onCameraStarted(camera: Camera) {
				frameWidth = cameraView.frameWidth
				frameHeight = cameraView.frameHeight
				frameOrientation = cameraView.frameOrientation
				camera.setPreviewCallback { data, _ -> frameData = data }
			}

			override fun onCameraStopping(camera: Camera) {
				camera.setPreviewCallback(null)
			}
		})

		lockOnView = (activity as MainActivity).lockOnView

		return view
	}

	override fun onDestroyView() {
		super.onDestroyView()
		yuvToGray.destroy()
	}

	override fun onResume() {
		super.onResume()
		System.gc()
		lockOnView.visibility = View.VISIBLE
		cameraView.visibility = View.VISIBLE
		cameraView.openAsync(CameraView.findCameraId(
				Camera.CameraInfo.CAMERA_FACING_BACK))
		startDecoding()
	}

	override fun onPause() {
		super.onPause()
		cancelDecoding()
		cameraView.close()
		cameraView.visibility = View.GONE
		lockOnView.visibility = View.GONE
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_camera, menu)
		if (!context.packageManager.hasSystemFeature(
				PackageManager.FEATURE_CAMERA_FLASH)) {
			menu.findItem(R.id.flash).setVisible(false)
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.flash -> {
				toggleTorchMode()
				true
			}
			R.id.info -> {
				openReadme()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun toggleTorchMode() {
		val camera = cameraView.getCamera()
		val parameters = camera?.getParameters()
		parameters?.setFlashMode(if (flash)
			Camera.Parameters.FLASH_MODE_OFF
		else
			Camera.Parameters.FLASH_MODE_TORCH)
		flash = flash xor true
		parameters?.let { camera.setParameters(parameters) }
	}

	private fun openReadme() {
		startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
				"https://github.com/markusfisch/BinaryEye/blob/master/README.md")))
	}

	private fun startDecoding() {
		activity ?: return

		frameData = null
		decoding = true
		decodingThread = Thread(decodingRunnable)
		decodingThread?.start()
	}

	private fun cancelDecoding() {
		decoding = false
		if (decodingThread != null) {
			var retry = 100
			while (retry-- > 0) {
				try {
					decodingThread?.join()
					break
				} catch (e: InterruptedException) {
					// try again
				}
			}
		}
	}

	private fun decodeFrame(): Result? {
		frameData ?: return null
		return zxing.decodeBitmap(yuvToGray.convert(
				frameData!!, frameWidth, frameHeight, frameOrientation))
	}

	private fun found(result: Result) {
		cancelDecoding()
		vibrator.vibrate(100)
		lockOnView.lock(getRelativeBounds(result.resultPoints), 250)
		lockOnView.postDelayed({
			addFragment(fragmentManager, DecodeFragment.newInstance(
					result.text,
					result.getBarcodeFormat()))
		}, 500)
	}

	private fun getRelativeBounds(resultPoints: Array<ResultPoint>): Rect {
		val fw: Float
		val fh: Float
		if (frameOrientation == 90 || frameOrientation == 270) {
			fw = frameHeight.toFloat()
			fh = frameWidth.toFloat()
		} else {
			fw = frameWidth.toFloat()
			fh = frameHeight.toFloat()
		}
		val viewRect = cameraView.previewRect
		val xf = fw / viewRect.width()
		val yf = fh / viewRect.height()
		val bounds = getBounds(resultPoints)
		bounds.set(
				Math.round(bounds.left / xf),
				Math.round(bounds.top / yf),
				Math.round(bounds.right / xf),
				Math.round(bounds.bottom / yf))
		bounds.offset(viewRect.left, viewRect.top)
		return bounds
	}

	private fun getBounds(resultPoints: Array<ResultPoint>): Rect {
		val bounds = Rect(0xffffff, 0xffffff, 0, 0)
		for (resultPoint in resultPoints) {
			val x = Math.round(resultPoint.x)
			val y = Math.round(resultPoint.y)
			bounds.set(
					Math.min(bounds.left, x),
					Math.min(bounds.top, y),
					Math.max(bounds.right, x),
					Math.max(bounds.bottom, y))
		}
		return bounds
	}

	private fun setZoom(zoom: Int) {
		val camera: Camera? = cameraView.getCamera()
		camera?.let {
			try {
				val params = camera.getParameters()
				params.setZoom(zoom)
				camera.setParameters(params)
			} catch (e: RuntimeException) {
				// ignore; there's nothing we can do
			}
		}
	}
}
