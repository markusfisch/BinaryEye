package de.markusfisch.android.binaryeye.actions.search

import android.content.Context
import android.content.Intent
import android.widget.Toast
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IAction
import de.markusfisch.android.binaryeye.app.alertDialog
import de.markusfisch.android.binaryeye.app.execShareIntent
import de.markusfisch.android.binaryeye.app.parseAndNormalizeUri
import de.markusfisch.android.binaryeye.app.prefs
import java.net.URLEncoder

object OpenOrSearchAction : IAction {
	override val iconResId: Int = R.drawable.ic_action_search
	override val titleResId: Int = R.string.search_web

	override fun canExecuteOn(data: ByteArray): Boolean = false

	override suspend fun execute(context: Context, data: ByteArray) {
		val intent = openUri(context, String(data)) ?: return
		execShareIntent(context, intent)
	}

	private suspend fun openUri(context: Context, data: String, search: Boolean = true): Intent? {
		val uri = parseAndNormalizeUri(data)
		val intent = Intent(Intent.ACTION_VIEW, uri)
		when {
			intent.resolveActivity(context.packageManager) != null -> return intent
			search -> return getSearchIntent(context, data)
			else -> Toast.makeText(
				context,
				R.string.cannot_resolve_action,
				Toast.LENGTH_SHORT
			).show()
		}
		return null
	}

	private suspend fun getSearchIntent(context: Context, query: String): Intent? {
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
		} ?: return null
		return openUri(context, queryUri, false)
	}
}
