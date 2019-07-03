package de.markusfisch.android.binaryeye.actions.wifi

object WifiNetworkFactory {
    private val regex = Regex("^WIFI:(T:(WEP|WPA|nopass))?;S:(.*);(P:(.*))?;(H?);$");

    fun parse(data: String): WifiNetworkInfo? {
        return regex.matchEntire(data)?.groupValues?.let {
            val security = it[2].let {
                if (it == "" || it == "nopass") return null
                it
            }

            val ssid = it[3]
            if (ssid.isEmpty()) return null

            val password = (if (it[5] == "") null else it[5])
            val hidden = it[6] == "H"
            return WifiNetworkInfo(
                    security,
                    ssid,
                    password,
                    hidden
            )
        }
    }
}

data class WifiNetworkInfo(
        val security: String,
        val ssid: String,
        val password: String?,
        val hidden: Boolean
)