package de.markusfisch.android.binaryeye.adapter

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.data.Database

class ScansAdapter(context: Context, cursor: Cursor) :
	CursorAdapter(context, cursor, false) {
	var selectedScanId = 0L
	var selectedScanPosition = -1

	private val idIndex = cursor.getColumnIndex(Database.SCANS_ID)
	private val timeIndex = cursor.getColumnIndex(Database.SCANS_DATETIME)
	private val nameIndex = cursor.getColumnIndex(Database.SCANS_NAME)
	private val contentIndex = cursor.getColumnIndex(Database.SCANS_CONTENT)
	private val formatIndex = cursor.getColumnIndex(Database.SCANS_FORMAT)

	fun select(id: Long, position: Int) {
		selectedScanId = id
		selectedScanPosition = position
	}

	fun clearSelection() {
		selectedScanId = 0L
		selectedScanPosition = -1
	}

	fun getSelectedContent() = if (selectedScanPosition < 0) {
		null
	} else {
		val cursor = getItem(selectedScanPosition)
		if (cursor is Cursor) {
			cursor.getString(contentIndex)
		} else {
			null
		}
	}

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
		val holder = getViewHolder(view)
		holder.timeView.text = cursor.getString(timeIndex)
		val name = cursor.getString(nameIndex)
		val content = cursor.getString(contentIndex)
		var icon = 0
		holder.contentView.text = when {
			name?.isNotEmpty() == true -> {
				icon = R.drawable.ic_label
				name
			}
			content?.isEmpty() == true -> context.getString(R.string.binary_data)
			else -> content
		}
		holder.contentView.setCompoundDrawablesWithIntrinsicBounds(
			icon, 0, 0, 0
		)
		holder.formatView.text = cursor.getString(formatIndex)
		// view.isSelected needs to be put on the queue to work
		val selected = cursor.getLong(idIndex) == selectedScanId
		view.post {
			view.isSelected = selected
		}
	}

	private fun getViewHolder(view: View): ViewHolder {
		return view.tag as ViewHolder? ?: ViewHolder(
			view.findViewById(R.id.time),
			view.findViewById(R.id.content),
			view.findViewById(R.id.format)
		).also {
			view.tag = it
		}
	}

	private data class ViewHolder(
		val timeView: TextView,
		val contentView: TextView,
		val formatView: TextView
	)
}
