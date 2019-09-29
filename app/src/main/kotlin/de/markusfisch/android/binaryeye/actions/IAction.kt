package de.markusfisch.android.binaryeye.actions

import android.content.Context
import android.content.Intent
import android.widget.Toast
import de.markusfisch.android.binaryeye.app.execShareIntent

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

fun IAction?.validateOrGetNew(data: ByteArray): IAction? {
	return this?.takeIf {
		canExecuteOn(data)
	} ?: ActionRegistry.getAction(data)
}
