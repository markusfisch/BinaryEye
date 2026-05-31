package de.markusfisch.android.binaryeye.actions.fido

import android.content.Context
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.Action
import de.markusfisch.android.binaryeye.content.openUrl

object FidoAction : Action() {
	private val fidoUriRegex = "^fido:/[0-9]+$".toRegex(
		RegexOption.IGNORE_CASE
	)

	override val iconResId: Int = R.drawable.ic_action_open
	override val titleResId: Int = R.string.open_url

	override fun canExecuteOn(data: ByteArray): Boolean {
		return String(data).trim().matches(fidoUriRegex)
	}

	override suspend fun execute(context: Context, data: ByteArray) {
		executed = context.openUrl(String(data).trim())
	}
}
