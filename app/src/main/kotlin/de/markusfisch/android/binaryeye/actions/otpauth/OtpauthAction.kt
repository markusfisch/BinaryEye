package de.markusfisch.android.binaryeye.actions.otpauth

import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.SchemeAction

object OtpauthAction : SchemeAction() {
	override val iconResId: Int = R.drawable.ic_action_otpauth
	override val titleResId: Int = R.string.otpauth_add
	override val scheme: String = "otpauth"
}
