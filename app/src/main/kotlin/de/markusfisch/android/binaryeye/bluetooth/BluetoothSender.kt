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
import java.io.OutputStreamWriter
import java.util.*

fun Scan.sendBluetoothAsync(
	host: String,
	callback: (Boolean, Boolean) -> Unit
) {
	CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
		val connected = if (isConnected) {
			true
		} else {
			connect(host)
		}
		val sent = if (connected) {
			send(content)
		} else {
			false
		}
		withContext(Dispatchers.Main) {
			callback(connected, sent)
		}
	}
}

fun setBluetoothHosts(listPref: ListPreference) {
	val devices = try {
		BluetoothAdapter.getDefaultAdapter().bondedDevices
	} catch (e: SecurityException) {
		// Do nothing, either the user has denied Bluetooth access
		// or the permission was removed by the system. We're catching
		// the exception to keep the app from crashing.
		null
	} ?: return
	listPref.entries = devices.map {
		it.name
	}.toTypedArray()
	listPref.entryValues = devices.map {
		it.address
	}.toTypedArray()
	listPref.callChangeListener(listPref.value)
}

private lateinit var socket: BluetoothSocket
private lateinit var writer: OutputStreamWriter

private val blue = BluetoothAdapter.getDefaultAdapter()
private val uuid = UUID.fromString(
	"8a8478c9-2ca8-404b-a0de-101f34ab71ae"
)

private var isConnected = false

private fun connect(deviceName: String): Boolean = try {
	val device = findByName(deviceName) ?: throw RuntimeException(
		"Bluetooth device not found"
	)
	socket = device.createRfcommSocketToServiceRecord(uuid)
	socket.connect()
	isConnected = true
	writer = socket.outputStream.writer()
	true
} catch (e: Exception) {
	try {
		close()
	} catch (e: Exception) {
		//Catch case when lateinit not initialized i.e. server not connected to before.
	}
	false
}

private fun send(message: String): Boolean = try {
	writer.apply {
		write(message)
		write("\n")
		flush()
	}
	true
} catch (e: Exception) {
	close()
	false
}

private fun close() {
	writer.close()
	socket.close()
	isConnected = false
}

private fun findByName(findableName: String): BluetoothDevice? {
	val deviceList = blue.bondedDevices
	for (device in deviceList) {
		if (device.address == findableName) {
			return device
		}
	}
	return null
}
