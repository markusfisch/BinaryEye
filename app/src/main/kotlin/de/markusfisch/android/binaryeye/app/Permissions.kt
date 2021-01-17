package de.markusfisch.android.binaryeye.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

var permissionGrantedCallback: (() -> Any)? = null

const val PERMISSION_CAMERA = 1
fun hasCameraPermission(activity: Activity) = hasPermission(
	activity,
	Manifest.permission.CAMERA,
	PERMISSION_CAMERA
)

const val PERMISSION_WRITE = 2
fun hasWritePermission(activity: Activity, callback: () -> Any): Boolean {
	permissionGrantedCallback = callback
	return hasPermission(
		activity,
		Manifest.permission.WRITE_EXTERNAL_STORAGE,
		PERMISSION_WRITE
	)
}

const val PERMISSION_LOCATION = 3
fun hasLocationPermission(activity: Activity, callback: () -> Any): Boolean {
	permissionGrantedCallback = callback
	return hasPermission(
		activity,
		Manifest.permission.ACCESS_FINE_LOCATION,
		PERMISSION_LOCATION
	)
}

private fun hasPermission(
	activity: Activity,
	permission: String,
	requestCode: Int
) = if (ContextCompat.checkSelfPermission(
		activity,
		permission
	) != PackageManager.PERMISSION_GRANTED
) {
	ActivityCompat.requestPermissions(
		activity,
		arrayOf(permission),
		requestCode
	)
	false
} else {
	true
}
