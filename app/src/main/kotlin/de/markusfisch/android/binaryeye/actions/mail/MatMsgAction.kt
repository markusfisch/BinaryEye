package de.markusfisch.android.binaryeye.actions.mail

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IntentAction

object MatMsgAction : IntentAction() {
	override val iconResId: Int = R.drawable.ic_action_mail
	override val titleResId: Int = R.string.mail_send
	override val errorMsg: Int = R.string.mail_error

	override fun canExecuteOn(data: ByteArray): Boolean {
		return String(data).startsWith("MATMSG:")
	}

	override suspend fun createIntent(
		context: Context,
		data: ByteArray
	): Intent? {
		val encoded = String(data)
		// Allow arbitrary order.
		val to = encoded.extractFirst("""[:;]TO:([\w.%@+-]+);""")
		val sub = encoded.extractFirst("""[:;]SUB:([^;]+);""")
		val body = encoded.extractFirst("""[:;]BODY:([^;]+);""")
		// Allow incomplete but not completely missing data.
		if (to == null && sub == null && body == null) {
			return null
		}
		return Intent(
			Intent.ACTION_SENDTO,
			Uri.parse("mailto:")
		).apply {
			to?.let {
				putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
			}
			sub?.let {
				putExtra(Intent.EXTRA_SUBJECT, sub)
			}
			body?.let {
				putExtra(Intent.EXTRA_TEXT, body)
			}
		}
	}
}

private fun String.extractFirst(regex: String) =
	regex.toRegex().find(this)?.groupValues?.get(1)
