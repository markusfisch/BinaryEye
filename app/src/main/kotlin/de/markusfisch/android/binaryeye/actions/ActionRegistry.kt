package de.markusfisch.android.binaryeye.actions

import de.markusfisch.android.binaryeye.actions.mail.MailAction
import de.markusfisch.android.binaryeye.actions.otpauth.OtpauthAction
import de.markusfisch.android.binaryeye.actions.search.OpenOrSearchAction
import de.markusfisch.android.binaryeye.actions.sms.SmsAction
import de.markusfisch.android.binaryeye.actions.tel.TelAction
import de.markusfisch.android.binaryeye.actions.vtype.vcard.VCardAction
import de.markusfisch.android.binaryeye.actions.vtype.vevent.VEventAction
import de.markusfisch.android.binaryeye.actions.web.WebAction
import de.markusfisch.android.binaryeye.actions.wifi.WifiAction

object ActionRegistry {
	val DEFAULT_ACTION: IAction = OpenOrSearchAction

	val REGISTRY: Set<IAction> = setOf(
		MailAction, OtpauthAction, SmsAction, TelAction, VCardAction, VEventAction, WebAction, WifiAction
	)

	fun getAction(data: ByteArray): IAction = REGISTRY.find {
		it.canExecuteOn(data)
	} ?: DEFAULT_ACTION
}
