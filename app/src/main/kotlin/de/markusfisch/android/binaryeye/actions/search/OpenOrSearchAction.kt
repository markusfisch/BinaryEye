package de.markusfisch.android.binaryeye.actions.search

import android.content.Context
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IAction
import de.markusfisch.android.binaryeye.app.alertDialog
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.content.openUrl
import de.markusfisch.android.binaryeye.net.urlEncode

object OpenOrSearchAction : IAction {
	override val iconResId: Int = R.drawable.ic_action_search
	override val titleResId: Int = R.string.search_web

	override fun canExecuteOn(data: ByteArray): Boolean = false

	override suspend fun execute(context: Context, data: ByteArray) {
		view(context, String(data), true)
	}

	private suspend fun view(
		context: Context,
		url: String,
		search: Boolean
	) {
		if (!context.openUrl(url, silent = true) && search) {
			openSearch(context, url)
		}
	}

	private suspend fun openSearch(context: Context, query: String) {
		val defaultSearchUrl = prefs.defaultSearchUrl
		if (defaultSearchUrl.isNotEmpty()) {
			view(
				context,
				defaultSearchUrl + query.urlEncode(),
				false
			)
			return
		}
		val names = context.resources.getStringArray(
			R.array.search_engines_names
		).toMutableList()
		val urls = context.resources.getStringArray(
			R.array.search_engines_values
		).toMutableList()
		// Remove the "Always ask" entry. The arrays search_engines_*
		// are used in the preferences too.
		names.removeFirst()
		urls.removeFirst()
		if (prefs.openWithUrl.isNotEmpty()) {
			names.add(prefs.openWithUrl)
			urls.add(prefs.openWithUrl)
		}
		val queryUri = alertDialog<String>(context) { resume ->
			setTitle(R.string.pick_search_engine)
			setItems(names.toTypedArray()) { _, which ->
				resume(urls[which] + query.urlEncode())
			}
		} ?: return
		view(context, queryUri, false)
	}
}
