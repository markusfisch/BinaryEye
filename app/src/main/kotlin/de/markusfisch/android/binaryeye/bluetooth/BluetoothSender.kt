package de.markusfisch.android.binaryeye.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.support.v7.preference.ListPreference
import de.markusfisch.android.binaryeye.database.Scan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.UUID

fun Scan.sendBluetoothAsync(
	host: String,
	callback: (Boolean, Boolean) -> Unit
) {
	CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
		val connected = if (isConnected) {
			true
		} else {
			connect(host, true)
		}
		val sent = if (connected) {
			send(text, host)
		} else {
			false
		}
		withContext(Dispatchers.Main) {
			callback(connected, sent)
		}
	}
}

fun setBluetoothHosts(listPref: ListPreference) {
	try {
		val devices = blue.bondedDevices ?: return
		listPref.entries = devices.map {
			it.name
		}.toTypedArray()
		listPref.entryValues = devices.map {
			it.address
		}.toTypedArray()
		listPref.callChangeListener(listPref.value)
	} catch (_: SecurityException) {
		// Do nothing, either the user has denied Bluetooth access
		// or the permission was removed by the system. We're catching
		// the exception to keep the app from crashing.
	}
}

private var socket: BluetoothSocket? = null
private var writer: OutputStreamWriter? = null

private val blue = BluetoothAdapter.getDefaultAdapter()
private val uuid = UUID.fromString(
	"8a8478c9-2ca8-404b-a0de-101f34ab71ae"
)

private var isConnected = false

private fun connect(deviceName: String, onceMore: Boolean): Boolean = try {
	val device = findByName(deviceName) ?: throw RuntimeException(
		"Bluetooth device not found"
	)
	socket = device.createRfcommSocketToServiceRecord(uuid)
	socket?.connect()
	writer = socket?.outputStream?.writer()
	isConnected = true
	true
} catch (_: Exception) {
	if (onceMore) {
		connect(deviceName, false)
	} else {
		close()
	}
	false
}

private fun send(message: String, host: String): Boolean = try {
	writer?.apply {
		write(message)
		write("\n")
		flush()
	}
	true
} catch (_: Exception) {
	close()

	if (connect(host, false)) {
		send(message, host)
	} else {
		false
	}
}

private fun close() {
	try {
		writer?.close()
	} catch (_: IOException) {
		// Catch exception if writer wasn't initialized.
	}
	try {
		socket?.close()
	} catch (_: IOException) {
		// Nothing we can do about this but keep
		// the app from crashing.
	}
	writer = null
	socket = null
	isConnected = false
}

private fun findByName(findableName: String): BluetoothDevice? {
	try {
		val deviceList = blue.bondedDevices
		for (device in deviceList) {
			if (device.address == findableName) {
				return device
			}
		}
	} catch (_: SecurityException) {
		// Fall through.
	}
	return null
}
