package de.markusfisch.android.binaryeye.data.csv

fun <T> csvBuilder(config: CSVBuilder<T>.() -> Unit): CSVBuilder<T> {
	return CSVBuilder<T>().apply(config)
}

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

		fun gettingByString(getter: (T) -> String): Unit = gettingBy {
			getter(it).toByteArray()
		}
	}

	fun buildWith(inputs: List<T>, delimiter: String): ByteArray {
		val header = columns.map {
			it.name?.escaped(delimiter)?.toByteArray()
				?: throw IllegalStateException("You need to provide a name for csv column")
		}.addDelimiter(delimiter).flatten()

		val contentLines = inputs.map { input ->
			columns.map {
				it.gettingBy?.run {
					var result = invoke(input)
					if (!it.isBinary) {
						result = String(result).escaped(delimiter).toByteArray()
					}
					return@run result
				} ?: throw IllegalStateException("You need to provide a getter for the value")
			}.addDelimiter(delimiter).flatten()
		}.addDelimiter("\n").flatten()

		return header + '\n'.toByte() + contentLines
	}

	private fun List<ByteArray>.addDelimiter(delimiter: String): List<ByteArray> {
		return mapIndexed { index, content ->
			if (index != size - 1) listOf(
				content,
				delimiter.toByteArray()
			) else listOf(content)
		}.flatten()
	}

	private fun List<ByteArray>.flatten(): ByteArray {
		val result = ByteArray(this.sumBy { it.size })
		var index = 0
		for (byteArray in this) {
			for (byte in byteArray) result[index++] = byte
		}
		return result
	}

	private fun Any.escaped(delimiter: String): String {
		val any = this.toString()
		return when {
			any.run { contains('"') || contains('\n') || contains(delimiter) } ->
				"\"${any.replace("\"", "\"\"").replace(delimiter, "\"$delimiter")}\""
			else -> any
		}
	}
}
