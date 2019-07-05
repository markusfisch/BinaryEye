package de.markusfisch.android.binaryeye.actions.mail

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.SimpleIntentIAction

object MailAction : SimpleIntentIAction() {
	private val mailRegex =
		"""^mail(?:to)?:([\w.%+-]+@[A-Za-z\d.-]+\.[A-Za-z]{2,6}(?:[?&](?:subject|body)=.*?){0,2}?)$""".toRegex()

	override val iconResId: Int = R.drawable.ic_action_mail
	override val titleResId: Int = R.string.mail_send
	override val errorMsg: Int = R.string.mail_error

	override fun canExecuteOn(data: ByteArray): Boolean {
		return String(data).matches(mailRegex)
	}

	override fun executeForIntent(context: Context, data: ByteArray): Intent? {
		val mailWithMessage = mailRegex.matchEntire(String(data))?.groupValues?.get(1) ?: return null
		return Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$mailWithMessage"))
	}
}
