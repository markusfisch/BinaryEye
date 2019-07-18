package de.markusfisch.android.binaryeye.actions.vtype.vevent

import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.SimpleIntentIAction
import de.markusfisch.android.binaryeye.actions.vtype.VTypeParser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import android.support.annotation.RequiresApi

import java.text.SimpleDateFormat
import java.util.Date

object VEventAction : SimpleIntentIAction() {
	override val iconResId: Int
		get() = R.drawable.ic_action_vevent
	override val titleResId: Int
		get() = R.string.vevent_add
	override val errorMsg: Int
		get() = R.string.vevent_failed

	override fun canExecuteOn(data: ByteArray): Boolean {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
				&& VTypeParser.parseVType(String(data)) == "VEVENT"
	}

	@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	override fun executeForIntent(context: Context, data: ByteArray): Intent? {
		val info = VTypeParser.parseMap(String(data))

		return Intent(Intent.ACTION_EDIT).apply {
			type = "vnd.android.cursor.item/event"
			info["SUMMARY"]?.singleOrNull()?.also { title ->
				putExtra(CalendarContract.Events.TITLE, title.value)
			}
			info["DESCRIPTION"]?.singleOrNull()?.also { description ->
				putExtra(CalendarContract.Events.DESCRIPTION, description.value)
			}
			info["LOCATION"]?.singleOrNull()?.also { location ->
				putExtra(CalendarContract.Events.EVENT_LOCATION, location.value)
			}
			info["DTSTART"]?.singleOrNull()?.also { eventStart ->
				dateFormats.simpleFindParse(eventStart.value)?.also {
					putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it.time)
				}
			}
			info["DTEND"]?.singleOrNull()?.also { eventEnd ->
				dateFormats.simpleFindParse(eventEnd.value)?.also {
					putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it.time)
				}
			}
		}
	}

	@SuppressLint("SimpleDateFormat") // we definitely don't wan't the local format
	private val dateFormats = listOf(
		SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
		SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
		SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'"),
		SimpleDateFormat("yyyyMMdd'T'HHmmss"),
		SimpleDateFormat("yyyy-MM-dd"),
		SimpleDateFormat("yyyyMMdd")
	)

	private fun List<SimpleDateFormat>.simpleFindParse(data: String): Date? {
		for (simpleDataFormat in this) {
			return simpleDataFormat.simpleParse(data) ?: continue
		}
		return null
	}

	private fun SimpleDateFormat.simpleParse(data: String): Date? = try {
		this.parse(data)
	} catch (e: Exception) {
		null
	}
}
