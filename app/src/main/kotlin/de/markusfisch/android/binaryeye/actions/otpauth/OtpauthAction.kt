package de.markusfisch.android.binaryeye.actions.otpauth

import android.content.Context
import android.content.Intent
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IntentAction

object OtpauthAction : IntentAction() {
	override val iconResId: Int = R.drawable.ic_action_otpauth
	override val titleResId: Int = R.string.otpauth_add
	override val errorMsg: Int = R.string.otpauth_error

	override fun canExecuteOn(data: ByteArray): Boolean {
		return OtpauthParser(String(data)) != null
	}

	override suspend fun createIntent(context: Context, data: ByteArray): Intent? {
		return OtpauthParser(String(data))?.let {
			Intent(Intent.ACTION_VIEW, it.uri)
		}
	}
}
