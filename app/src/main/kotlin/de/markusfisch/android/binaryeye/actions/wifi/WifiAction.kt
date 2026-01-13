package de.markusfisch.android.binaryeye.actions.wifi

import android.content.Context
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.Action
import de.markusfisch.android.binaryeye.widget.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WifiAction : Action() {
	override val iconResId = R.drawable.ic_action_wifi
	override val titleResId = R.string.connect_to_wifi

	var password: String? = null

	override fun canExecuteOn(data: ByteArray): Boolean {
		// Just check if the data can be parsed as the app should also
		// show invalid configurations so users can still access them.
		val inputMap = WifiConnector.parseMap(String(data)) ?: return false
		password = inputMap["P"]
		return !inputMap["S"].isNullOrEmpty()
	}

	override suspend fun execute(context: Context, data: ByteArray) {
		withContext(Dispatchers.IO) {
			fired = WifiConnector.parse(String(data))?.let {
				WifiConnector.addNetwork(context, it)
			} ?: false
			withContext(Dispatchers.Main) {
				context.toast(
					if (fired) {
						R.string.wifi_added
					} else {
						R.string.wifi_config_failed
					}
				)
			}
		}
	}
}
