package de.markusfisch.android.binaryeye.actions.sms

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IntentAction

object SmsAction : IntentAction() {
	private val smsRegex = """^sms(?:to)?:(\+?[0-9]+)(?::([\S\s]*))?$""".toRegex(
		RegexOption.IGNORE_CASE
	)

	override val iconResId: Int = R.drawable.ic_action_sms
	override val titleResId: Int = R.string.sms_send
	override val errorMsg: Int = R.string.sms_error

	override fun canExecuteOn(data: ByteArray): Boolean {
		return String(data).matches(smsRegex)
	}

	override suspend fun createIntent(context: Context, data: ByteArray): Intent? {
		val (number: String, message: String) = smsRegex.matchEntire(
			String(data)
		)?.let {
			it.groupValues[1] to it.groupValues[2]
		} ?: return null
		return Intent(
			Intent.ACTION_SENDTO,
			Uri.parse("smsto:$number")
		).apply {
			if (message.isNotEmpty()) {
				putExtra("sms_body", message)
			}
		}
	}
}
