package de.markusfisch.android.binaryeye.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.preference.ListPreference
import de.markusfisch.android.binaryeye.database.Scan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.UUID

fun Scan.sendBluetoothAsync(
	host: String,
	callback: (Boolean, Boolean) -> Unit
) {
	CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
		val (connected, sent) = connectionLock.withLock {
			if (connectedHost != host) {
				close()
			}
			val connected = connectedHost == host ||
				connect(host) || connect(host)
			connected to (connected && (send(text) ||
				(connect(host) && send(text))))
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

private val connectionLock = Mutex()
private var connectedHost: String? = null

private fun connect(host: String): Boolean = try {
	val device = findByName(host) ?: throw IOException("Bluetooth device not found")
	val connection = device.createRfcommSocketToServiceRecord(uuid)
	socket = connection
	connection.connect()
	writer = connection.outputStream.writer()
	connectedHost = host
	true
} catch (_: Exception) {
	close()
	false
}

private fun send(message: String): Boolean = try {
	val output = checkNotNull(writer)
	output.write(message)
	output.write("\n")
	output.flush()
	true
} catch (_: Exception) {
	close()
	false
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
	connectedHost = null
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
