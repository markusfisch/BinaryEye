package de.markusfisch.android.binaryeye.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import de.markusfisch.android.binaryeye.database.Scan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.util.*

fun Scan.sendBluetoothAsync(
    host : String,
    callback: (Boolean, Boolean) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO)
    {
        //TODO needs to be standardized differently
        var connectResponse = true
        var sendResponse = false

        if (!BluetoothSender.isConnected())
            connectResponse = BluetoothSender.connect(host)

        if (connectResponse) {
            sendResponse = BluetoothSender.send(content)
        }

        withContext(Dispatchers.Main) {
            callback(connectResponse, sendResponse)
        }
    }
}


private class BluetoothSender
{
    companion object {
        //non-deprecated call requires api lvl 18
        private val blue: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        lateinit private var socket: BluetoothSocket
        lateinit private var writer: OutputStreamWriter
        private val uuid = UUID.fromString("8a8478c9-2ca8-404b-a0de-101f34ab71ae")
        //using socket builtin function require api lvl 14
        private var isConnected = false

        fun connect(deviceName: String) : Boolean {
            try {
                val device = findByName(deviceName)
                socket = device!!.createRfcommSocketToServiceRecord(uuid)
                socket.connect()
                isConnected = true
                writer = socket.outputStream.writer()
            } catch(e: Exception) {
                close()
                return false
            }

            return true
        }

        fun send(message: String) : Boolean {
            return try {
                writer.write(message)
                writer.write("\n")
                writer.flush()
                true
            } catch(e: Exception) {
                close()
                false
            }
        }

        fun close() {
            writer.close()
            socket.close()
            isConnected = false
        }

        fun isConnected() : Boolean {
            return isConnected
            //return socket.isConnected
//            if(socket != null)
//                return true
//            else
//                return false
        }

        private fun findByName(findableName : String) : BluetoothDevice?
        {
            val deviceList = blue.bondedDevices
            for(device in deviceList)
            {
                if (device.address == findableName)
                    return device
            }
            return null
        }
    }
}