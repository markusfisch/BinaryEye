package de.markusfisch.android.binaryeye.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

var permissionGrantedCallback: (() -> Any)? = null

const val PERMISSION_CAMERA = 1
fun Activity.hasCameraPermission() = hasPermission(
	Manifest.permission.CAMERA,
	PERMISSION_CAMERA
)

const val PERMISSION_WRITE = 2
fun Activity.hasWritePermission(callback: () -> Any): Boolean {
	permissionGrantedCallback = callback
	return hasPermission(
		Manifest.permission.WRITE_EXTERNAL_STORAGE,
		PERMISSION_WRITE
	)
}

const val PERMISSION_LOCATION = 3
fun Activity.hasLocationPermission(callback: () -> Any): Boolean {
	permissionGrantedCallback = callback
	return hasPermission(
		Manifest.permission.ACCESS_FINE_LOCATION,
		PERMISSION_LOCATION
	)
}

const val PERMISSION_BLUETOOTH = 4
fun Activity.hasBluetoothPermission() :Boolean {
	return hasPermission(
		Manifest.permission.BLUETOOTH_CONNECT,
		PERMISSION_BLUETOOTH
	)
}


private fun Activity.hasPermission(
	permission: String,
	requestCode: Int
) = if (ContextCompat.checkSelfPermission(
		this,
		permission
	) != PackageManager.PERMISSION_GRANTED
) {
	ActivityCompat.requestPermissions(
		this,
		arrayOf(permission),
		requestCode
	)
	false
} else {
	true
}
