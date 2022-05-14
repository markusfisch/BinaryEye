package de.markusfisch.android.binaryeye.actions.web

import android.content.Context
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IAction
import de.markusfisch.android.binaryeye.content.openUrl

object WebAction : IAction {
	private val colloquialRegex = """^(http[s]*://)*([a-z0-9]+[a-z0-9-]*[a-z0-9]+[.])+[a-z]{2,}([?#/]+[\w/=&;._-]*)*$""".toRegex(
		RegexOption.IGNORE_CASE
	)

	override val iconResId: Int = R.drawable.ic_action_open
	override val titleResId: Int = R.string.open_url

	override fun canExecuteOn(data: ByteArray): Boolean {
		return String(data).trim().matches(colloquialRegex)
	}

	override suspend fun execute(context: Context, data: ByteArray) {
		var url = String(data).trim()
		if (!url.startsWith("http") && !url.startsWith("ftp")) {
			url = "http://${url}"
		}
		context.openUrl(url)
	}
}
