package de.markusfisch.android.binaryeye.actions.search

import android.content.Context
import android.content.Intent
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IAction
import de.markusfisch.android.binaryeye.app.alertDialog
import de.markusfisch.android.binaryeye.app.parseAndNormalizeUri
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.content.execShareIntent
import de.markusfisch.android.binaryeye.content.startIntent
import de.markusfisch.android.binaryeye.widget.toast
import java.net.URLEncoder

object OpenOrSearchAction : IAction {
	override val iconResId: Int = R.drawable.ic_action_search
	override val titleResId: Int = R.string.search_web

	override fun canExecuteOn(bytes: ByteArray): Boolean = false

	override suspend fun execute(context: Context, bytes: ByteArray) {
		view(context, String(bytes), true)
	}

	private suspend fun view(context: Context, s: String, search: Boolean) {
		val intent = Intent(Intent.ACTION_VIEW, parseAndNormalizeUri(s))
		if (!context.startIntent(intent) && search) {
			openSearch(context, s)
		}
	}

	private suspend fun openSearch(context: Context, query: String) {
		val names = context.resources.getStringArray(
			R.array.search_engines_names
		).toMutableList()
		val urls = context.resources.getStringArray(
			R.array.search_engines_values
		).toMutableList()
		if (prefs.openWithUrl.isNotEmpty()) {
			names.add(prefs.openWithUrl)
			urls.add(prefs.openWithUrl)
		}
		val queryUri = alertDialog<String>(context) { resume ->
			setTitle(R.string.pick_search_engine)
			setItems(names.toTypedArray()) { _, which ->
				resume(urls[which] + URLEncoder.encode(query, "utf-8"))
			}
		} ?: return
		view(context, queryUri, false)
	}
}
