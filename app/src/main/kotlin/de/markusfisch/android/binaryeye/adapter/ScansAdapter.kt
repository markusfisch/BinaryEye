package de.markusfisch.android.binaryeye.adapter

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CursorAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.database.Database
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class ScansAdapter(
	context: Context,
	cursor: Cursor,
	private val onPinClick: (Long, Boolean) -> Unit
) :
	CursorAdapter(context, cursor, false) {
	private val idIndex = cursor.getColumnIndex(Database.SCANS_ID)
	private val pinnedIndex = cursor.getColumnIndex(Database.SCANS_PINNED)
	private val timeIndex = cursor.getColumnIndex(Database.SCANS_DATETIME)
	private val nameIndex = cursor.getColumnIndex(Database.SCANS_NAME)
	private val textIndex = cursor.getColumnIndex(Database.SCANS_TEXT)
	private val formatIndex = cursor.getColumnIndex(Database.SCANS_FORMAT)
	private val selections = mutableMapOf<Long, Int>()
	private val selectedColor = ContextCompat.getColor(
		context, R.color.selected_row
	)

	fun select(view: View, id: Long, position: Int) {
		view.select(
			if (selections[id] == null) {
				selections[id] = position
				true
			} else {
				selections.remove(id)
				false
			}
		)
	}

	fun clearSelection() {
		selections.clear()
	}

	fun forSelection(callback: (Long, Int) -> Unit) {
		selections.forEach {
			callback.invoke(it.key, it.value)
		}
	}

	fun getSelectedIds(): List<Long> = mutableListOf<Long>().apply {
		forSelection { id, _ ->
			add(id)
		}
	}

	fun getSelectedContent(separator: String): String {
		val sb = StringBuilder()
		forSelection { _, position ->
			getContent(position)?.let {
				if (sb.isNotEmpty()) {
					sb.append(separator)
				}
				sb.append(it)
			}
		}
		return sb.toString()
	}

	fun getName(
		position: Int
	) = (getItem(position) as Cursor?)?.getString(nameIndex)

	fun getContent(
		position: Int
	) = (getItem(position) as Cursor?)?.getString(textIndex)

	override fun newView(
		context: Context,
		cursor: Cursor,
		parent: ViewGroup
	): View? = LayoutInflater.from(parent.context).inflate(
		R.layout.row_scan, parent, false
	)

	override fun bindView(
		view: View,
		context: Context,
		cursor: Cursor
	) {
		val id = cursor.getLong(idIndex)
		val pinned = cursor.getInt(pinnedIndex) != 0
		val format = cursor.getString(formatIndex)
		getViewHolder(view).apply {
			metaView.text = context.getString(
				R.string.scan_meta_data,
				formatDateTime(cursor.getString(timeIndex)),
				format.prettifyFormatName()
			)
			formatIconView.setImageResource(format.toBarcodeIcon())
			val name = cursor.getString(nameIndex)
			val text = cursor.getString(textIndex)
			var icon = 0
			contentView.text = when {
				name?.isNotEmpty() == true -> {
					icon = R.drawable.ic_label
					name
				}

				text.isNullOrEmpty() -> context.getString(
					R.string.binary_data
				)

				else -> text
			}
			contentView.setCompoundDrawablesWithIntrinsicBounds(
				icon, 0, 0, 0
			)
			pinView.apply {
				setImageResource(
					if (pinned) {
						R.drawable.ic_action_pin
					} else {
						R.drawable.ic_action_pin_outline
					}
				)
				contentDescription = context.getString(
					if (pinned) {
						R.string.unpin_item
					} else {
						R.string.pin_item
					}
				)
				setOnClickListener {
					onPinClick(id, !pinned)
				}
			}
		}
		val selected = selections[id] != null
		view.post {
			// Needs to be put on the queue to work.
			view.select(selected)
		}
	}

	private fun View.select(selected: Boolean) {
		setBackgroundColor(if (selected) selectedColor else 0)
	}

	private fun getViewHolder(
		view: View
	): ViewHolder = view.tag as ViewHolder? ?: ViewHolder(
		view.findViewById(R.id.format_icon),
		view.findViewById(R.id.meta),
		view.findViewById(R.id.content),
		view.findViewById(R.id.pin)
	).also {
		view.tag = it
	}

	private data class ViewHolder(
		val formatIconView: ImageView,
		val metaView: TextView,
		val contentView: TextView,
		val pinView: ImageButton
	)
}

private val formatWordBoundary =
	"(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=[A-Za-z])(?=\\d)|(?<=\\d)(?=[A-Za-z])".toRegex()

fun Context.setupFormatSpinner(spinner: Spinner): List<String> {
	val names = resources.getStringArray(
		R.array.barcode_formats_names
	).toMutableList().apply {
		add(0, getString(R.string.all_formats))
	}
	val values = resources.getStringArray(
		R.array.barcode_formats_values
	).toMutableList().apply {
		add(0, "")
	}
	spinner.adapter = ArrayAdapter(
		this,
		android.R.layout.simple_spinner_item,
		names
	).apply {
		setDropDownViewResource(
			android.R.layout.simple_spinner_dropdown_item
		)
	}
	return values
}

fun String.prettifyFormatName() = replace("_", " ")
	.replace(formatWordBoundary, " ")

fun String.toFormatDescriptionResId(): Int = when (this) {
	"Aztec" -> R.string.format_description_aztec
	"Codabar" -> R.string.format_description_codabar
	"Code39" -> R.string.format_description_code39
	"Code39Ext" -> R.string.format_description_code39ext
	"Code32" -> R.string.format_description_code32
	"PZN" -> R.string.format_description_pzn
	"Code93" -> R.string.format_description_code93
	"Code128" -> R.string.format_description_code128
	"DataBar" -> R.string.format_description_databar
	"DataBarOmni" -> R.string.format_description_databar_omni
	"DataBarStk" -> R.string.format_description_databar_stk
	"DataBarLtd" -> R.string.format_description_databar_ltd
	"DataBarExp" -> R.string.format_description_databar_exp
	"DataBarExpStk" -> R.string.format_description_databar_exp_stk
	"DataMatrix" -> R.string.format_description_data_matrix
	"DXFilmEdge" -> R.string.format_description_dx_film_edge
	"EAN8" -> R.string.format_description_ean8
	"EAN13" -> R.string.format_description_ean13
	"ITF" -> R.string.format_description_itf
	"MaxiCode" -> R.string.format_description_maxicode
	"PDF417" -> R.string.format_description_pdf417
	"QRCode" -> R.string.format_description_qr_code
	"MicroQRCode" -> R.string.format_description_micro_qr_code
	"RMQRCode" -> R.string.format_description_rmqr_code
	"UPCA" -> R.string.format_description_upca
	"UPCE" -> R.string.format_description_upce
	else -> 0
}

private fun String?.toBarcodeIcon(): Int = when (this) {
	"QRCode",
	"RMQRCode" -> R.drawable.ic_barcode_qr_code

	"MicroQRCode" -> R.drawable.ic_barcode_micro_qr

	"Aztec" -> R.drawable.ic_barcode_aztec
	"DataMatrix",
	"MaxiCode" -> R.drawable.ic_barcode_data_matrix

	"PDF417" -> R.drawable.ic_barcode_pdf417

	else -> R.drawable.ic_barcode_linear
}

private fun formatDateTime(rfc: String): String {
	try {
		SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(rfc)?.let {
			return DateFormat.getDateTimeInstance(
				DateFormat.LONG,
				DateFormat.SHORT
			).format(it)
		}
	} catch (_: ParseException) {
		// Ignore and fall through.
	}
	return rfc
}
