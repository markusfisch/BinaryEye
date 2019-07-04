package de.markusfisch.android.binaryeye.actions

import de.markusfisch.android.binaryeye.actions.wifi.WifiAction

object ActionRegistry {
    val REGISTRY : Array<IAction> = arrayOf(
            WifiAction
    )

    fun getAction(data: ByteArray): IAction? = REGISTRY.find { it.canExecuteOn(data) }
}