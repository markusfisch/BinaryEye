package de.markusfisch.android.binaryeye.data.csv

import de.markusfisch.android.binaryeye.app.addDelimiter
import de.markusfisch.android.binaryeye.app.asFlowWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
fun <T> csvBuilder(config: CSVBuilder<T>.() -> Unit): CSVBuilder<T> {
	return CSVBuilder<T>().apply(config)
}

@ExperimentalCoroutinesApi
class CSVBuilder<T> {
	private val columns = mutableListOf<ColumnBuilder>()

	fun column(config: ColumnBuilder.() -> Unit): ColumnBuilder {
		return ColumnBuilder().apply(config).also {
			columns.add(it)
		}
	}

	inner class ColumnBuilder {
		var isBinary = false
		var name: String? = null
		internal var gettingBy: ((T) -> ByteArray)? = null

		fun gettingBy(getter: (T) -> ByteArray) {
			gettingBy = getter
		}

		fun gettingByString(getter: (T) -> String?): Unit = gettingBy {
			getter(it)?.toByteArray() ?: ByteArray(0)
		}
	}

	@FlowPreview
	fun buildWith(inputs: Flow<T>, delimiter: String): Flow<ByteArray> = flow {
		val columnsFlow = columns.asFlow()
		val header = columnsFlow.buildHeader(delimiter)
		val contentLines = inputs.buildContent(columnsFlow, delimiter)

		emitAll(header)
		emit("\n".toByteArray())
		emitAll(contentLines)
	}

	private fun Flow<ColumnBuilder>.buildHeader(delimiter: String) = map {
		it.name?.escaped(delimiter)?.toByteArray()
			?: throw IllegalStateException("You need to provide a name for csv column")
	}.addDelimiter(delimiter)

	@FlowPreview
	private fun Flow<T>.buildContent(
		columnsFlow: Flow<ColumnBuilder>,
		delimiter: String
	): Flow<ByteArray> = map { input ->
		input.getLine(columnsFlow, delimiter)
	}.addDelimiter("\n".toByteArray()) { other: Flow<ByteArray> ->
		// this: ByteArray
		this asFlowWith other
	}.flattenConcat()

	private fun T.getLine(columnsFlow: Flow<ColumnBuilder>, delimiter: String): Flow<ByteArray> =
		columnsFlow.map { column ->
			column.gettingBy?.run {
				var result = invoke(this@getLine)
				if (!column.isBinary) {
					result = String(result).escaped(delimiter).toByteArray()
				}
				return@run result
			} ?: throw IllegalStateException("You need to provide a getter for the value")
		}.addDelimiter(delimiter)

	private fun Any.escaped(delimiter: String): String {
		val any = this.toString()
		return when {
			any.run { contains('"') || contains('\n') || contains(delimiter) } ->
				"\"${any.replace("\"", "\"\"").replace(delimiter, "\"$delimiter")}\""
			else -> any
		}
	}
}
