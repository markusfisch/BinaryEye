package de.markusfisch.android.binaryeye.actions.wifi

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IAction


object WifiAction : IAction {
    override val resourceId = R.drawable.ic_action_wifi

    override fun canExecuteOn(data: ByteArray): Boolean = WifiNetworkFactory.parse(String(data)) != null

    override fun execute(context: Context, data: ByteArray) {
        // TODO: support unsecured networks
        WifiNetworkFactory.parse(String(data))?.let {
            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = String.format("\"%s\"", it.ssid)
            if (it.password != null) {
                wifiConfig.preSharedKey = String.format("\"%s\"", it.password)
            }
            wifiConfig.hiddenSSID = it.hidden

            val wifiManager = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val netId = wifiManager.addNetwork(wifiConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
        }
    }
}