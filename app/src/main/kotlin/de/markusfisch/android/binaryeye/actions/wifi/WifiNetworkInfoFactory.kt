package de.markusfisch.android.binaryeye.actions.wifi

object WifiNetworkFactory {
    private val wifiRegex = Regex("^WIFI:(T:(WEP|WPA|nopass))?;S:(.*);(P:(.*))?;(H?);$")
    private val hexRegex = Regex("^[0-9a-f]*$", RegexOption.IGNORE_CASE)

    fun parse(data: String): WifiNetworkInfo? {
        return wifiRegex.matchEntire(data)?.groupValues?.let {
            val security = it[2].let {
                if (it == "" || it == "nopass") return null
                it
            }

            val ssid = quoteUnlessHex(it[3])
            if (ssid.isEmpty()) return null

            val password = if (it[5] == "") null else quoteUnlessHex(it[5])

            val hidden = it[6] == "H"
            return WifiNetworkInfo(
                    security,
                    ssid,
                    password,
                    hidden
            )
        }
    }

    private fun quoteUnlessHex(str: String): String {
        if (hexRegex.matches(str)) return str

        return String.format("\"%s\"", str)
    }
}

data class WifiNetworkInfo(
        val security: String,
        val ssid: String,
        val password: String?,
        val hidden: Boolean
)