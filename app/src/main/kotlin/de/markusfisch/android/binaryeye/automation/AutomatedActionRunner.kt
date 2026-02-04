package de.markusfisch.android.binaryeye.automation

import android.content.Context
import android.content.Intent
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.content.execShareIntent
import de.markusfisch.android.binaryeye.content.openUrl
import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.binaryeye.widget.toast

fun Context.runAutomatedActions(scan: Scan): Boolean {
	val content = scan.text
	if (content.isEmpty()) {
		return false
	}
	val actions = prefs.automatedActions
	if (actions.isEmpty()) {
		return false
	}
	for (action in actions) {
		if (action.matches(content)) {
			return when (action.type) {
				AutomatedAction.Type.Intent -> {
					val url = action.urlTemplate?.buildUrl(scan)
					if (url.isNullOrEmpty()) {
						toast(R.string.automated_action_invalid_url)
						false
					} else {
						openUrl(url)
					}
				}

				AutomatedAction.Type.CustomIntent ->
					execCustomIntent(action, scan)
			}
		}
	}
	return false
}

private fun Context.execCustomIntent(
	action: AutomatedAction,
	scan: Scan
): Boolean {
	val template = action.intentUriTemplate
	if (template.isNullOrEmpty()) {
		toast(R.string.automated_action_intent_uri_required)
		return false
	}
	val intentUri = template.buildTemplate(scan, encodeResult = false)
	val intent = try {
		Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
	} catch (_: Exception) {
		toast(R.string.automated_action_intent_uri_invalid)
		return false
	}
	return execShareIntent(intent)
}
