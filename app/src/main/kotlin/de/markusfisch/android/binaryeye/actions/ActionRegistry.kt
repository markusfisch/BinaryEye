package de.markusfisch.android.binaryeye.actions

object ActionRegistry {
    val REGISTRY : Array<IAction> = arrayOf(
            WifiAction
    )

    fun getAction(data: ByteArray): IAction? = REGISTRY.find { it.canExecuteOn(data) }
}