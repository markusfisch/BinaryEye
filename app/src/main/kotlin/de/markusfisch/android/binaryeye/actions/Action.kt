package de.markusfisch.android.binaryeye.actions

import android.content.Context
import android.content.Intent
import de.markusfisch.android.binaryeye.content.execShareIntent
import de.markusfisch.android.binaryeye.content.openUrl
import de.markusfisch.android.binaryeye.widget.toast

abstract class Action {
	abstract val iconResId: Int
	abstract val titleResId: Int

	var executed: Boolean = false

	abstract fun canExecuteOn(data: ByteArray): Boolean
	abstract suspend fun execute(context: Context, data: ByteArray)
}

abstract class IntentAction : Action() {
	abstract val errorMsg: Int

	final override suspend fun execute(context: Context, data: ByteArray) {
		val intent = createIntent(context, data)
		if (intent == null) {
			context.toast(errorMsg)
		} else {
			executed = context.execShareIntent(intent)
		}
	}

	abstract suspend fun createIntent(
		context: Context,
		data: ByteArray
	): Intent?
}

abstract class SchemeAction : Action() {
	abstract val scheme: String
	open val buildRegex: Boolean = false

	final override fun canExecuteOn(data: ByteArray): Boolean {
		val content = String(data)
		return if (buildRegex) {
			content.matches(
				"""^$scheme://[\w\W]+$""".toRegex(
					RegexOption.IGNORE_CASE
				)
			)
		} else {
			content.startsWith("$scheme://", ignoreCase = true)
		}
	}

	final override suspend fun execute(context: Context, data: ByteArray) {
		executed = context.openUrl(String(data))
	}
}
