package de.markusfisch.android.binaryeye.actions

import android.content.Context

interface IAction {
	val iconResId: Int
	val titleResId: Int

	fun canExecuteOn(data: ByteArray): Boolean
	fun execute(context: Context, data: ByteArray)
}

fun IAction?.validateOrGetNew(data: ByteArray): IAction? {
	return this?.takeIf { canExecuteOn(data) } ?: ActionRegistry.getAction(data)
}
