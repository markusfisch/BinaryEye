package de.markusfisch.android.binaryeye.preference

import android.content.SharedPreferences
import android.graphics.Point
import org.json.JSONObject

fun SharedPreferences.storeCropHandle(
	name: String,
	viewWidth: Int,
	viewHeight: Int,
	cropHandle: Point
) {
	edit().putString(
		cropHandleKey(name, viewWidth, viewHeight),
		JSONObject().apply {
			put("x", cropHandle.x)
			put("y", cropHandle.y)
		}.toString()
	).apply()
}

fun SharedPreferences.restoreCropHandle(
	name: String,
	viewWidth: Int,
	viewHeight: Int,
	default: Point = Point(-2, -2) // -2 means set default roi.
): Point {
	val s = getString(
		cropHandleKey(name, viewWidth, viewHeight), null
	) ?: return default
	return try {
		val json = JSONObject(s)
		Point(
			json.getInt("x"),
			json.getInt("y")
		)
	} catch (_: Exception) {
		return default
	}
}

private fun cropHandleKey(
	name: String,
	viewWidth: Int,
	viewHeight: Int
) = "${name}:${viewWidth}:${viewHeight}"
