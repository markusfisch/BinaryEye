package de.markusfisch.android.binaryeye.actions

import android.content.Context

interface IAction {
	val resourceId: Int

	fun canExecuteOn(data: ByteArray): Boolean
	fun execute(context: Context, data: ByteArray)
}
