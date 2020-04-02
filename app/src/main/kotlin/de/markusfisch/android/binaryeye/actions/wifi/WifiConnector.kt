package de.markusfisch.android.binaryeye.actions.wifi

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiEnterpriseConfig
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.support.annotation.RequiresApi
import android.widget.Toast
import de.markusfisch.android.binaryeye.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

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
object WifiConnector {
	fun parse(input: String): Any? {
		val inputMap = parseMap(input) ?: return null
		val parsedData = SimpleDataAccessor.of(inputMap) ?: return null
		return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			// WifiConfiguration is deprecated in Android Q
			@Suppress("DEPRECATION")
			WifiConfiguration().apply(parsedData)
		} else {
			WifiNetworkSuggestion.Builder().apply(parsedData)
		}
	}

	suspend fun connect(context: Context, config: Any) {
		val wifiManager = context.applicationContext.getSystemService(
			Context.WIFI_SERVICE
		) as WifiManager
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			// WifiConfiguration is deprecated in Android Q
			@Suppress("DEPRECATION")
			val wifiConfig = config as WifiConfiguration
			wifiManager.enableWifi(context)
			wifiManager.removeOldNetwork(wifiConfig)
			wifiManager.enableNewNetwork(wifiConfig)
			toastSuccess(context)
		} else {
			val suggestion = (config as WifiNetworkSuggestion.Builder).build()
			if (wifiManager.addNetworkSuggestions(listOf(suggestion)) ==
				WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
			) {
				toastSuccess(context)
			}
		}
	}

	internal fun parseMap(string: String): Map<String, String>? {
		// normally those codes should have the last semicolon, but many
		// generators don't add it
		val wifiRegex = """^WIFI:((?:.+?:(?:[^\\;]|\\.)*;)+);?$""".toRegex()
		// should be: ^(.+):((?:[^\\;,":]|\\.)*);$ but allows unescaped , "
		// and : because many QR Code creators don't escape properly
		val pairRegex = """(.+?):((?:[^\\;]|\\.)*);""".toRegex()
		return wifiRegex.matchEntire(
			string
		)?.groupValues?.get(1)?.let { pairs ->
			pairRegex.findAll(pairs).map { pair ->
				pair.groupValues[1].toUpperCase(Locale.US) to pair.groupValues[2]
			}.toMap()
		}
	}

	internal class SimpleDataAccessor private constructor(
		private val inputMap: Map<String, String>
	) {
		// this should be private but because of testing it isn't possible
		internal val ssid: String
			get() = inputMap.getValue("S").unescaped
		internal val securityType: String
			get() = inputMap["T"]?.unescaped ?: ""
		internal val password: String?
			get() = inputMap["P"]?.unescaped
		internal val hidden: Boolean
			get() = inputMap["H"]?.unescaped == "true"
		internal val anonymousIdentity: String
			get() = inputMap["AI"]?.unescaped ?: ""
		internal val identity: String
			get() = inputMap["I"]?.unescaped ?: ""
		internal val eapMethod: Int?
			get() = if (inputMap["E"].isNullOrEmpty()) {
				requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) {
					WifiEnterpriseConfig.Eap.NONE
				}
			} else when (inputMap["E"]) {
				"AKA" -> requireSdk(Build.VERSION_CODES.LOLLIPOP) { WifiEnterpriseConfig.Eap.AKA }
				"AKA_PRIME" -> requireSdk(Build.VERSION_CODES.M) { WifiEnterpriseConfig.Eap.AKA_PRIME }
				"NONE" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.NONE }
				"PEAP" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.PEAP }
				"PWD" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.PWD }
				"SIM" -> requireSdk(Build.VERSION_CODES.LOLLIPOP) { WifiEnterpriseConfig.Eap.SIM }
				"TLS" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.TLS }
				"TTLS" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Eap.TTLS }
				"UNAUTH_TLS" -> requireSdk(Build.VERSION_CODES.N) { WifiEnterpriseConfig.Eap.UNAUTH_TLS }
				else -> null
			}
		internal val phase2Method: Int?
			get() = if (inputMap["PH2"].isNullOrEmpty()) {
				requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) {
					WifiEnterpriseConfig.Phase2.NONE
				}
			} else when (inputMap["PH2"]) {
				"AKA" -> requireSdk(Build.VERSION_CODES.O) { WifiEnterpriseConfig.Phase2.AKA }
				"AKA_PRIME" -> requireSdk(Build.VERSION_CODES.O) { WifiEnterpriseConfig.Phase2.AKA_PRIME }
				"GTC" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.GTC }
				"MSCHAP" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.MSCHAP }
				"MSCHAPV2" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.MSCHAPV2 }
				"NONE" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.NONE }
				"PAP" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) { WifiEnterpriseConfig.Phase2.PAP }
				"SIM" -> requireSdk(Build.VERSION_CODES.O) { WifiEnterpriseConfig.Phase2.SIM }
				else -> null
			}

		internal companion object {
			internal fun of(inputMap: Map<String, String>): SimpleDataAccessor? {
				return SimpleDataAccessor(inputMap).takeUnless {
					inputMap["S"].isNullOrEmpty()
				}
			}
		}
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private fun WifiNetworkSuggestion.Builder.apply(
		data: SimpleDataAccessor
	): WifiNetworkSuggestion.Builder? {
		fun WifiNetworkSuggestion.Builder.applyCommon(
			data: SimpleDataAccessor
		): WifiNetworkSuggestion.Builder? {
			setSsid(data.ssid)
			return this
		}

		fun WifiNetworkSuggestion.Builder.applySecurity(
			data: SimpleDataAccessor
		): WifiNetworkSuggestion.Builder? {
			when (data.securityType) {
				"WPA" -> {
					data.password?.let {
						setWpa2Passphrase(it)
					}
				}
				"WPA2-EAP" -> {
					data.password?.let { setWpa2Passphrase(it) }
					setWpa2EnterpriseConfig(WifiEnterpriseConfig().apply {
						identity = data.identity
						anonymousIdentity = data.anonymousIdentity
						password = data.password
						data.eapMethod?.let { eapMethod = it }
						data.phase2Method?.let { phase2Method = it }
					})
				}
				"WPA3" -> {
					data.password?.let {
						setWpa3Passphrase(it)
					}
				}
				"WPA3-EAP" -> {
					data.password?.let { setWpa3Passphrase(it) }
					setWpa3EnterpriseConfig(WifiEnterpriseConfig().apply {
						identity = data.identity
						anonymousIdentity = data.anonymousIdentity
						password = data.password
						data.eapMethod?.let { eapMethod = it }
						data.phase2Method?.let { phase2Method = it }
					})
				}
			}
			return this
		}

		return this.applyCommon(data)?.applySecurity(data)
	}

	// WifiConfiguration is deprecated in Android Q
	@Suppress("DEPRECATION")
	private fun WifiConfiguration.apply(
		data: SimpleDataAccessor
	): WifiConfiguration? {
		val ssidWithQuotes = data.ssid.quotedUnlessHex
		val passwordWithQuotes = data.password?.quotedUnlessHex

		fun WifiConfiguration.applyCommon(
			data: SimpleDataAccessor
		): WifiConfiguration? {
			allowedAuthAlgorithms.clear()
			allowedGroupCiphers.clear()
			allowedKeyManagement.clear()
			allowedPairwiseCiphers.clear()
			allowedProtocols.clear()

			SSID = ssidWithQuotes
			hiddenSSID = data.hidden
			return this
		}

		fun WifiConfiguration.applySecurity(
			data: SimpleDataAccessor
		): WifiConfiguration? {
			when (data.securityType) {
				"", "nopass" -> allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
				"WEP" -> @Suppress("DEPRECATION") /* WEP as insecure */ {
					passwordWithQuotes?.let {
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
				"WPA" -> {
					passwordWithQuotes?.let {
						preSharedKey = it
					} ?: return null
					allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
					@Suppress("DEPRECATION") // WPA 1 is insecure and has bad performance
					allowedProtocols.set(WifiConfiguration.Protocol.WPA) // WPA
					allowedProtocols.set(WifiConfiguration.Protocol.RSN) // WPA2
					allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
					allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP)
					@Suppress("DEPRECATION") // TKIP is insecure and has bad performance
					allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
					allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
					allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
					allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
				}
				"WPA2-EAP" -> requireSdk(Build.VERSION_CODES.JELLY_BEAN_MR2) {
					passwordWithQuotes?.let {
						preSharedKey = it
					} ?: return null
					allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
					allowedProtocols.set(WifiConfiguration.Protocol.RSN) // WPA2
					allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP)
					@Suppress("DEPRECATION") // TKIP is insecure and has bad performance
					allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
					allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
					allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
					allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)

					enterpriseConfig.identity = data.identity
					enterpriseConfig.anonymousIdentity = data.anonymousIdentity
					enterpriseConfig.password = data.password
					enterpriseConfig.eapMethod =
						data.eapMethod ?: return null // non valid eapMethod
					enterpriseConfig.phase2Method =
						data.phase2Method ?: return null // non valid phase2Method
				} ?: return null // api isn't high enough
			}
			return this
		}

		return this.applyCommon(data)?.applySecurity(data)
	}

	private inline fun <T> requireSdk(version: Int, block: () -> T): T? {
		return if (Build.VERSION.SDK_INT >= version) return block() else null
	}
}

private suspend fun toastSuccess(context: Context) {
	withContext(Dispatchers.Main) {
		Toast.makeText(
			context,
			R.string.wifi_added,
			Toast.LENGTH_LONG
		).show()
	}
}

// keep possibility of wrongly unescaped \ by explicitly searching
// for special chars
private val escapedRegex = """\\([\\;,":])""".toRegex()
private val String.unescaped: String
	get() = this.replace(escapedRegex) { escaped ->
		escaped.groupValues[1]
	}

private val hexRegex = """^[0-9a-f]+$""".toRegex(
	RegexOption.IGNORE_CASE
)
private val String.quotedUnlessHex: String
	get() = if (matches(hexRegex) || (startsWith("\"") &&
				endsWith("\""))
	) this else "\"$this\""

private fun WifiManager.enableWifi(context: Context): Boolean {
	if (!isWifiEnabled) {
		// setWifiEnabled() will always return false for Android Q
		// because Q doesn't allow apps to enable/disable Wi-Fi anymore
		@Suppress("DEPRECATION")
		if (!setWifiEnabled(true)) {
			Toast.makeText(
				context,
				R.string.wifi_config_failed,
				Toast.LENGTH_LONG
			).show()
			return false
		}
	}
	return true
}

// WifiConfiguration is deprecated in Android Q
@Suppress("DEPRECATION")
private fun WifiManager.removeOldNetwork(
	wifiConfig: WifiConfiguration
) {
	configuredNetworks?.firstOrNull {
		it.SSID == wifiConfig.SSID &&
				it.allowedKeyManagement == wifiConfig.allowedKeyManagement
	}?.networkId?.also {
		removeNetwork(it)
	}
}

// WifiConfiguration is deprecated in Android Q
@Suppress("DEPRECATION")
private fun WifiManager.enableNewNetwork(
	wifiConfig: WifiConfiguration
) {
	val id = addNetwork(wifiConfig)
	disconnect()
	enableNetwork(id, true)
	reconnect()
}
