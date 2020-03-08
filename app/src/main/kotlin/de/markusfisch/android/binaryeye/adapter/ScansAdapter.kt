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
	private val timeIndex = cursor.getColumnIndex(Database.SCANS_DATETIME)
	private val contentIndex = cursor.getColumnIndex(Database.SCANS_CONTENT)
	private val formatIndex = cursor.getColumnIndex(Database.SCANS_FORMAT)

	override fun newView(
		context: Context,
		cursor: Cursor,
		parent: ViewGroup
	) = LayoutInflater.from(parent.context).inflate(
		R.layout.row_scan, parent, false
	)

	override fun bindView(
		view: View,
		context: Context,
		cursor: Cursor
	) {
		val holder = getViewHolder(view)
		val content = cursor.getString(contentIndex)
		holder.timeView.text = cursor.getString(timeIndex)
		holder.contentView.text = if (content.isEmpty()) {
			context.getString(R.string.binary_data)
		} else {
			content
		}
		holder.formatView.text = cursor.getString(formatIndex)
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
