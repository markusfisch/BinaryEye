package de.markusfisch.android.binaryeye.actions.web

import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.SchemeAction

object WebAction : SchemeAction() {
	override val iconResId: Int = R.drawable.ic_action_open
	override val titleResId: Int = R.string.open_url
	override val scheme: String = "https?"
	override val buildRegex: Boolean = true
}
