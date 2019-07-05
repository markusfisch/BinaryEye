package de.markusfisch.android.binaryeye.actions

import de.markusfisch.android.binaryeye.actions.mail.MailAction
import de.markusfisch.android.binaryeye.actions.sms.SmsAction
import de.markusfisch.android.binaryeye.actions.tel.TelAction
import de.markusfisch.android.binaryeye.actions.wifi.WifiAction

object ActionRegistry {
	val REGISTRY: Set<IAction> = setOf(
			WifiAction, SmsAction, TelAction, MailAction
	)

	fun getAction(data: ByteArray): IAction? = REGISTRY.find { it.canExecuteOn(data) }
}
