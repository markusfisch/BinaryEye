package de.markusfisch.android.binaryeye.actions

import android.content.Context
import android.content.Intent
import android.widget.Toast
import de.markusfisch.android.binaryeye.R

interface IAction {
	val iconResId: Int
	val titleResId: Int

	fun canExecuteOn(data: ByteArray): Boolean
	fun execute(context: Context, data: ByteArray)
}

abstract class SimpleIntentIAction : IAction {
	abstract val errorMsg: Int

	final override fun execute(context: Context, data: ByteArray) {
		val intent = executeForIntent(context, data)
				?: return Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
		if (intent.resolveActivity(context.packageManager) != null) {
			context.startActivity(intent)
		} else {
			Toast.makeText(context, R.string.cannot_resolve_action, Toast.LENGTH_LONG).show()
		}
	}

	abstract fun executeForIntent(context: Context, data: ByteArray): Intent?
}

fun IAction?.validateOrGetNew(data: ByteArray): IAction? {
	return this?.takeIf { canExecuteOn(data) } ?: ActionRegistry.getAction(data)
}
