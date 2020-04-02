package de.markusfisch.android.binaryeye.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

fun hasCameraPermission(activity: Activity, requestCode: Int = 1): Boolean {
	return hasPermission(activity, Manifest.permission.CAMERA, requestCode)
}

fun hasWritePermission(activity: Activity, requestCode: Int = 2): Boolean {
	return hasPermission(
		activity,
		Manifest.permission.WRITE_EXTERNAL_STORAGE,
		requestCode
	)
}

fun hasLocationPermission(activity: Activity, requestCode: Int = 3): Boolean {
	return hasPermission(
		activity,
		Manifest.permission.ACCESS_FINE_LOCATION,
		requestCode
	)
}

fun hasPermission(
	activity: Activity,
	permission: String,
	requestCode: Int
): Boolean {
	return if (ContextCompat.checkSelfPermission(activity, permission) !=
		PackageManager.PERMISSION_GRANTED
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
}
