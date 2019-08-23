package de.markusfisch.android.binaryeye.actions.tel

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.SimpleIntentIAction

object TelAction : SimpleIntentIAction() {
	private val telRegex = """^tel:(\+?[0-9]+)$""".toRegex(RegexOption.IGNORE_CASE)

	override val iconResId: Int = R.drawable.ic_action_tel
	override val titleResId: Int = R.string.tel_dial
	override val errorMsg: Int = R.string.tel_error

	override fun canExecuteOn(data: ByteArray): Boolean {
		return String(data).matches(telRegex)
	}

	override fun executeForIntent(context: Context, data: ByteArray): Intent {
		return Intent(Intent.ACTION_DIAL, Uri.parse(String(data)))
	}
}
