package de.markusfisch.android.binaryeye.actions.search

import android.content.Context
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.Action
import de.markusfisch.android.binaryeye.app.alertDialog
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.content.openUrl
import de.markusfisch.android.binaryeye.net.urlEncode

object OpenOrSearchAction : Action() {
	override val iconResId: Int = R.drawable.ic_action_search
	override val titleResId: Int = R.string.search_web

	override fun canExecuteOn(data: ByteArray): Boolean = false

	override suspend fun execute(context: Context, data: ByteArray) {
		fired = openUrlOrSearch(context, String(data))
	}
}

private suspend fun openUrlOrSearch(
	context: Context,
	url: String,
	search: Boolean = true
): Boolean {
	if (context.openUrl(url, silent = true)) {
		return true
	}
	if (search) {
		return prependSearchUrl(context, url)?.let {
			openUrlOrSearch(context, it, false)
		} ?: false
	}
	return false
}

private suspend fun prependSearchUrl(
	context: Context,
	query: String
): String? {
	val defaultSearchUrl = prefs.defaultSearchUrl
	if (defaultSearchUrl.isNotEmpty()) {
		return defaultSearchUrl + query.urlEncode()
	}
	val names = context.resources.getStringArray(
		R.array.search_engines_names
	).toMutableList()
	val urls = context.resources.getStringArray(
		R.array.search_engines_values
	).toMutableList()
	// Remove the "Always ask" entry. The arrays search_engines_*
	// are used in the preferences too.
	names.removeAt(0)
	urls.removeAt(0)
	if (prefs.openWithUrl.isNotEmpty()) {
		names.add(prefs.openWithUrl)
		urls.add(prefs.openWithUrl)
	}
	return alertDialog(context) { resume ->
		setTitle(R.string.pick_search_engine)
		setItems(names.toTypedArray()) { _, which ->
			resume(urls[which] + query.urlEncode())
		}
	}
}
