package de.markusfisch.android.binaryeye.preference

import android.content.SharedPreferences
import org.json.JSONObject

data class CropHandle(
	val x: Int = -2, // -2 means set default roi.
	val y: Int = -2,
	val orientation: Int = 0
)

fun SharedPreferences.storeCropHandle(
	name: String,
	cropHandle: CropHandle
) {
	edit().putString(
		name,
		JSONObject().apply {
			put("x", cropHandle.x)
			put("y", cropHandle.y)
			put("orientation", cropHandle.orientation)
		}.toString()
	).apply()
}

fun SharedPreferences.restoreCropHandle(
	name: String,
	default: CropHandle = CropHandle()
): CropHandle {
	val s = getString(name, null) ?: return default
	return try {
		val json = JSONObject(s)
		CropHandle(
			json.getInt("x"),
			json.getInt("y"),
			json.getInt("orientation")
		)
	} catch (_: Exception) {
		return default
	}
}
