package de.markusfisch.android.binaryeye.actions

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import de.markusfisch.android.binaryeye.R


object WifiAction : IAction {
    private val regex = Regex("^WIFI:T:(WEP|WPA|nopass|);S:(.*);P:(.*);(H?);$");

    override val resourceId = R.drawable.ic_action_wifi

    override fun canExecuteOn(data: ByteArray): Boolean = regex.matches(String(data))

    override fun execute(context: Context, data: ByteArray) {
        // TODO: support unsecured networks
        regex.matchEntire(String(data))?.groupValues?.let {
            // val security = it[1]
            val ssid = it[2]
            val password = it[3]
            val hidden = it[4] == "H"

            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = String.format("\"%s\"", ssid)
            wifiConfig.preSharedKey = String.format("\"%s\"", password)
            wifiConfig.hiddenSSID = hidden

            val wifiManager = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val netId = wifiManager.addNetwork(wifiConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
        }
    }
}