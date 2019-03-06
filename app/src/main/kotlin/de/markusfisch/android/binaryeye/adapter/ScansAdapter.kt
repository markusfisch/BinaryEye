package de.markusfisch.android.binaryeye.adapter

import de.markusfisch.android.binaryeye.data.Database
import de.markusfisch.android.binaryeye.R

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView

class ScansAdapter(context: Context, cursor: Cursor) :
	CursorAdapter(context, cursor, false) {
	private val timeIndex = cursor.getColumnIndex(Database.SCANS_DATETIME)
	private val contentIndex = cursor.getColumnIndex(Database.SCANS_CONTENT)
	private val formatIndex = cursor.getColumnIndex(Database.SCANS_FORMAT)

	override fun newView(
		context: Context,
		cursor: Cursor,
		parent: ViewGroup
	): View {
		return LayoutInflater.from(parent.context).inflate(
			R.layout.row_scan, parent, false
		)
	}

	override fun bindView(
		view: View,
		context: Context,
		cursor: Cursor
	) {
		val holder = getViewHolder(view)
		holder.timeView.text = cursor.getString(timeIndex)
		holder.contentView.text = cursor.getString(contentIndex)
		holder.formatView.text = cursor.getString(formatIndex)
	}

	private fun getViewHolder(view: View): ViewHolder {
		var holder = view.tag as ViewHolder?
		if (holder == null) {
			holder = ViewHolder(
				view.findViewById(R.id.time),
				view.findViewById(R.id.content),
				view.findViewById(R.id.format)
			)
			view.tag = holder
		}
		return holder
	}

	private data class ViewHolder(
		val timeView: TextView,
		val contentView: TextView,
		val formatView: TextView
	)
}
