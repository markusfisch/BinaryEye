package de.markusfisch.android.binaryeye.actions.wifi

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiEnterpriseConfig
import android.os.Build

/**
 * Normal:
 * WIFI:S:[network SSID];T:<WPA|WEP|nopass|>;P:[network password];H:<true|false|>;;
 *
 * WPA2 enterprise (EAP):
 * WIFI:S:[network SSID];T:WPA2-EAP;H:<true|false|nopass|>;E:[EAP method];PH2:[Phase 2 method];AI:[anonymous identity];I:[identity];P:[password];;
 *
 * EPA methods:
 *      "AKA",      "AKA_PRIME",
 *      "NONE",     "PEAP",
 *      "PWD",      "SIM",
 *      "TLS",      "TTLS",
 *      "UNAUTH_TLS"
 * https://developer.android.com/reference/android/net/wifi/WifiEnterpriseConfig.Eap.html
 *
 * Phase 2 methods:
 *      "AKA",      "AKA_PRIME",
 *      "GTC",      "MSCHAP",
 *      "MSCHAPV2", "NONE",
 *      "PAP",      "SIM"
 * https://developer.android.com/reference/android/net/wifi/WifiEnterpriseConfig.Phase2.html
 *
 * The fields can appear in any order. Only "S:" is required.
 */
class WifiConfigurationFactory internal constructor(input: String) {
	// normally those codes should have the last semicolon, but many generators don't add it
	private val wifiRegex = """^WIFI:(.+:(?:[^\\;]|\\.)*;)+;?$""".toRegex()
	// should be: ^(.+):((?:[^\\;,":]|\\.)*);$ but allows unescaped , " and : because many qr creator doesn't escape
	private val pairRegex = """(.+?):((?:[^\\;]|\\.)*);""".toRegex()

	private val hexRegex = """^[0-9a-f]+$""".toRegex(RegexOption.IGNORE_CASE)
	// keep possibility of wrongly not escaped \ by explicitly searching for special chars
	private val escapedRegex = """\\([\\;,":])""".toRegex()

	private val inputMap = input.parseWifiMap() // Map [S to SSID, T to SECURITY]

	private fun String.parseWifiMap(): Map<String, String> {
		return wifiRegex.matchEntire(this)?.groupValues?.get(1)?.let { pairs ->
			pairRegex.findAll(pairs).map { pair ->
				pair.groupValues[1].toUpperCase() to pair.groupValues[2].unescaped
			}.toMap()
		} ?: mapOf() // format error -- will be catched by "constructor"
	}

	// this should be private but because of testing it isn't possible
	internal val ssid: String
		get() = inputMap.getValue("S").quotedUnlessHex
	internal val securityType: String
		get() = inputMap["T"] ?: ""
	internal val password: String?
		get() = inputMap["P"]?.quotedUnlessHex
	internal val hidden: Boolean
		get() = inputMap["H"] == "true"
	internal val anonymousIdentity: String
		get() = inputMap["AI"] ?: ""
	internal val identity: String
		get() = inputMap["I"] ?: ""
	internal val eapMethod: Int?
		get() = if (inputMap["E"].isNullOrEmpty()) {
			requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.NONE }
		} else when (inputMap["E"]) {
			"AKA" ->        requireSdk(Build.VERSION_CODES.LOLLIPOP)       { WifiEnterpriseConfig.Eap.AKA }
			"AKA_PRIME" ->  requireSdk(Build.VERSION_CODES.M)              { WifiEnterpriseConfig.Eap.AKA_PRIME }
			"NONE" ->       requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.NONE }
			"PEAP" ->       requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.PEAP }
			"PWD" ->        requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.PWD }
			"SIM" ->        requireSdk(Build.VERSION_CODES.LOLLIPOP)       { WifiEnterpriseConfig.Eap.SIM }
			"TLS" ->        requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.TLS }
			"TTLS" ->       requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.TTLS }
			"UNAUTH_TLS" -> requireSdk(Build.VERSION_CODES.N)              { WifiEnterpriseConfig.Eap.UNAUTH_TLS }
			else -> null
		}
	internal val phase2Method: Int?
		get() = if (inputMap["PH2"].isNullOrEmpty()) {
			requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.NONE }
		} else when (inputMap["PH2"]) {
			"AKA" ->       requireSdk(Build.VERSION_CODES.O)              { WifiEnterpriseConfig.Phase2.AKA }
			"AKA_PRIME" -> requireSdk(Build.VERSION_CODES.O)              { WifiEnterpriseConfig.Phase2.AKA_PRIME }
			"GTC" ->       requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.GTC }
			"MSCHAP" ->    requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.MSCHAP }
			"MSCHAPV2" ->  requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.MSCHAPV2 }
			"NONE" ->      requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.NONE }
			"PAP" ->       requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.PAP }
			"SIM" ->       requireSdk(Build.VERSION_CODES.O)              { WifiEnterpriseConfig.Phase2.SIM }
			else -> null
		}

	private lateinit var wifiConfig: WifiConfiguration

	private fun setupCommonProperties() : WifiConfigurationFactory {
		wifiConfig = WifiConfiguration().apply {
			allowedAuthAlgorithms.clear()
			allowedGroupCiphers.clear()
			allowedKeyManagement.clear()
			allowedPairwiseCiphers.clear()
			allowedProtocols.clear()

			SSID = ssid
			hiddenSSID = hidden
		}
		return this
	}

	private fun setupSecurity(): WifiConfigurationFactory? {
		when (securityType) {
			"", "nopass" -> wifiConfig.apply {
				allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
			}
			"WEP" -> @Suppress("DEPRECATION") /* WEP as insecure */ wifiConfig.apply {
				password?.also {
					wepKeys[0] = it
				} ?: return null
				wepTxKeyIndex = 0
				allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
				allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
				allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
				allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
				allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
				allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
			}
			"WPA" -> wifiConfig.apply {
				password?.also {
					preSharedKey = it
				} ?: return null
				allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
				allowedProtocols.set(WifiConfiguration.Protocol.WPA) // WPA
				allowedProtocols.set(WifiConfiguration.Protocol.RSN) // WPA2
				allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
				allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP)
				allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
				allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
				allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
				allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
			}
			"WPA2-EAP" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) {
				wifiConfig.apply {
					password?.also {
						preSharedKey = it
					} ?: return null
					allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
					allowedProtocols.set(WifiConfiguration.Protocol.RSN) // WPA2
					allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP)
					allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
					allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
					allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
					allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)

					enterpriseConfig.identity = identity
					enterpriseConfig.anonymousIdentity = anonymousIdentity
					enterpriseConfig.password = password
					enterpriseConfig.eapMethod = eapMethod ?: return null // non valid eapMethod
					enterpriseConfig.phase2Method = phase2Method ?: return null // non valid phase2Method
				}
			} ?: return null // api isn't high enough
		}
		return this
	}

	private val String.unescaped: String
		get() = this.replace(escapedRegex) { escaped ->
			escaped.groupValues[1]
		}

	private val String.quotedUnlessHex: String
		get() = if (matches(hexRegex) || (startsWith("\"") && endsWith("\""))) this else "\"$this\""

	private inline fun <T> requireSdk(version: Int, getter: () -> T): T? {
		return if (Build.VERSION.SDK_INT >= version) return getter() else null
	}

	companion object {
		fun parse(input: String): WifiConfiguration? = WifiConfigurationFactory(input).takeIf {
			!it.inputMap["S"].isNullOrEmpty() && it.setupCommonProperties().setupSecurity() != null
		}?.wifiConfig
	}
}
