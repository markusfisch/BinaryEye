package de.markusfisch.android.binaryeye.actions

import android.content.Context
import android.content.Intent
import android.widget.Toast
import de.markusfisch.android.binaryeye.app.execShareIntent
import de.markusfisch.android.binaryeye.app.parseAndNormalizeUri

interface IAction {
	val iconResId: Int
	val titleResId: Int

	fun canExecuteOn(data: ByteArray): Boolean
	suspend fun execute(context: Context, data: ByteArray)
}

abstract class IntentAction : IAction {
	abstract val errorMsg: Int

	final override suspend fun execute(context: Context, data: ByteArray) {
		val intent =
			createIntent(context, data) ?: return Toast.makeText(
				context,
				errorMsg,
				Toast.LENGTH_LONG
			).show()
		execShareIntent(context, intent)
	}

	abstract suspend fun createIntent(context: Context, data: ByteArray): Intent?
}

abstract class SchemeAction : IAction {
	abstract val scheme: String
	open val intentAction: String = Intent.ACTION_VIEW
	open val buildRegex: Boolean = false

	final override fun canExecuteOn(data: ByteArray): Boolean {
		val content = String(data)
		return if (buildRegex) {
			content.matches("""^$scheme://[\w\W]+$""".toRegex())
		} else {
			content.startsWith("$scheme://")
		}
	}

	final override suspend fun execute(context: Context, data: ByteArray) {
		val uri = parseAndNormalizeUri(String(data))
		execShareIntent(context, Intent(intentAction, uri))
	}
}
