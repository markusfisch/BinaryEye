package de.markusfisch.android.binaryeye.actions.vtype.vevent

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import android.support.annotation.RequiresApi
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IntentAction
import de.markusfisch.android.binaryeye.actions.vtype.VTypeParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VEventAction : IntentAction() {
	override val iconResId: Int
		get() = R.drawable.ic_action_vevent
	override val titleResId: Int
		get() = R.string.vevent_add
	override val errorMsg: Int
		get() = R.string.vevent_failed

	override fun canExecuteOn(data: ByteArray): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return false
		}
		val type = VTypeParser.parseVType(String(data))
		return type == "VEVENT" || type == "VCALENDAR"
	}

	@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	override suspend fun createIntent(context: Context, data: ByteArray): Intent? {
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
					putExtra(
						CalendarContract.EXTRA_EVENT_BEGIN_TIME,
						it.time
					)
				}
			}
			info["DTEND"]?.singleOrNull()?.also { eventEnd ->
				dateFormats.simpleFindParse(eventEnd.value)?.also {
					putExtra(
						CalendarContract.EXTRA_EVENT_END_TIME,
						it.time
					)
				}
			}
		}
	}
}

private val dateFormats = listOf(
	"yyyy-MM-dd'T'HH:mm:ssXXX",
	"yyyy-MM-dd'T'HH:mm:ssZ",
	"yyyy-MM-dd'T'HH:mm:ssz",
	"yyyy-MM-dd'T'HH:mm:ss",
	"yyyyMMdd'T'HHmmssXXX",
	"yyyyMMdd'T'HHmmssZ",
	"yyyyMMdd'T'HHmmssz",
	"yyyyMMdd'T'HHmmss",
	"yyyy-MM-dd",
	"yyyyMMdd"
)

private fun List<String>.simpleFindParse(date: String): Date? {
	for (pattern in this) {
		return SimpleDateFormat(
			pattern,
			Locale.getDefault()
		).simpleParse(date) ?: continue
	}
	return null
}

private fun SimpleDateFormat.simpleParse(date: String): Date? = try {
	parse(date)
} catch (e: Exception) {
	null
}
