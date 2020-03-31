package de.markusfisch.android.binaryeye.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.scanReduce

@ExperimentalCoroutinesApi
fun Flow<ByteArray>.addDelimiter(delimiter: String) = addDelimiter(
	delimiter.toByteArray(),
	ByteArray::plus
)

@ExperimentalCoroutinesApi
inline fun <T, R> Flow<T>.addDelimiter(
	delimiter: R,
	crossinline append: R.(T) -> T
): Flow<T> = scanReduce { _, content ->
	delimiter.append(content)
}

@ExperimentalCoroutinesApi
infix fun <T> T.asFlowWith(other: Flow<T>) = flow<T> {
	emit(this@asFlowWith)
	emitAll(other)
}
