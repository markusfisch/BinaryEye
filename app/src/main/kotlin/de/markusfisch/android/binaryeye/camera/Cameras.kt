package de.markusfisch.android.binaryeye.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.core.CameraSelector
import de.markusfisch.android.binaryeye.R

const val DEFAULT_CAMERA_BACK = "back"
const val DEFAULT_CAMERA_FRONT = "front"
const val DEFAULT_CAMERA_OTHER = "other"

fun availableDefaultCameras(context: Context): List<String> {
	val lensFacings = try {
		(context.getSystemService(Context.CAMERA_SERVICE) as CameraManager)
			.run {
				cameraIdList.mapNotNull { id ->
					getCameraCharacteristics(id)
						.get(CameraCharacteristics.LENS_FACING)
				}
			}.toSet()
	} catch (_: Exception) {
		emptySet()
	}
	return buildList {
		if (lensFacings.isEmpty() ||
			lensFacings.contains(CameraCharacteristics.LENS_FACING_BACK)
		) {
			add(DEFAULT_CAMERA_BACK)
		}
		if (lensFacings.contains(CameraCharacteristics.LENS_FACING_FRONT)) {
			add(DEFAULT_CAMERA_FRONT)
		}
		if (lensFacings.contains(CameraCharacteristics.LENS_FACING_EXTERNAL)) {
			add(DEFAULT_CAMERA_OTHER)
		}
	}
}

fun defaultCameraEntries(context: Context, values: List<String>) =
	values.map {
		when (it) {
			DEFAULT_CAMERA_FRONT -> context.getString(R.string.default_camera_front)
			DEFAULT_CAMERA_OTHER -> context.getString(R.string.default_camera_other)
			else -> context.getString(R.string.default_camera_back)
		}
	}.toTypedArray()

@SuppressLint("UnsafeOptInUsageError")
fun cameraSelector(camera: String) = CameraSelector.Builder()
	.requireLensFacing(
		when (camera) {
			DEFAULT_CAMERA_FRONT -> CameraSelector.LENS_FACING_FRONT
			DEFAULT_CAMERA_OTHER -> CameraSelector.LENS_FACING_EXTERNAL
			else -> CameraSelector.LENS_FACING_BACK
		}
	)
	.build()
