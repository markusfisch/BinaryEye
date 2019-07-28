package de.markusfisch.android.binaryeye.fragment

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.adapter.ScansAdapter
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.app.shareText
import de.markusfisch.android.binaryeye.data.Database
import de.markusfisch.android.binaryeye.R

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView

import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
	private lateinit var listView: ListView
	private lateinit var fab: View
	private lateinit var progressView: View

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View {
		activity.setTitle(R.string.history)

		val view = inflater.inflate(
			R.layout.fragment_history,
			container,
			false
		)

		val useHistorySwitch = view.findViewById(
			R.id.use_history
		) as SwitchCompat
		initHistorySwitch(useHistorySwitch)

		listView = view.findViewById(R.id.scans)
		listView.emptyView = useHistorySwitch
		listView.setOnItemClickListener { _, _, _, id ->
			showScan(id)
		}
		listView.setOnItemLongClickListener { _, v, _, id ->
			askToRemoveScan(v.context, id)
			true
		}

		fab = view.findViewById(R.id.share)
		fab.setOnClickListener { v ->
			pickListSeparatorAndShare(v.context)
		}

		progressView = view.findViewById(R.id.progress_view)

		return view
	}

	override fun onDestroy() {
		super.onDestroy()
		(listView.adapter as ScansAdapter?)?.changeCursor(null)
	}

	override fun onResume() {
		super.onResume()
		update(context)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_history, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.clear -> {
				askToRemoveAllScans(context)
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun update(context: Context) {
		GlobalScope.launch {
			val cursor = db.getScans()
			GlobalScope.launch(Main) {
				fab.visibility = if (cursor != null && cursor.count > 0) {
					View.VISIBLE
				} else {
					View.GONE
				}
				cursor?.let {
					if (listView.adapter == null) {
						listView.adapter = ScansAdapter(context, cursor)
					} else {
						val adapter = listView.adapter as ScansAdapter
						adapter.changeCursor(cursor)
						adapter.notifyDataSetChanged()
					}
				}
			}
		}
	}

	private fun showScan(id: Long) {
		val cursor = db.getScan(id)
		if (cursor != null && cursor.moveToFirst()) {
			val contentIndex = cursor.getColumnIndex(Database.SCANS_CONTENT)
			val rawIndex = cursor.getColumnIndex(Database.SCANS_RAW)
			val formatIndex = cursor.getColumnIndex(Database.SCANS_FORMAT)
			try {
				addFragment(
					fragmentManager,
					DecodeFragment.newInstance(
						cursor.getString(contentIndex),
						BarcodeFormat.valueOf(cursor.getString(formatIndex)),
						cursor.getBlob(rawIndex)
					)
				)
			} catch (e: IllegalArgumentException) {
				// shouldn't ever happen
			}
		}
		cursor?.close()
	}

	private fun askToRemoveScan(context: Context, id: Long) {
		AlertDialog.Builder(context)
			.setMessage(R.string.really_remove_scan)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				db.removeScan(id)
				update(context)
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
	}

	private fun askToRemoveAllScans(context: Context) {
		AlertDialog.Builder(context)
			.setMessage(R.string.really_remove_all_scans)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				db.removeScans()
				update(context)
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
	}

	private fun pickListSeparatorAndShare(context: Context) {
		val separators = context.resources.getStringArray(
			R.array.list_separators_values
		)
		AlertDialog.Builder(context)
			.setTitle(R.string.pick_list_separator)
			.setItems(R.array.list_separators_names) { _, which ->
				shareScans(separators[which])
			}
			.show()
	}

	private fun shareScans(separator: String) {
		if (progressView.visibility == View.VISIBLE) {
			return
		}
		progressView.visibility = View.VISIBLE
		GlobalScope.launch {
			val cursor = db.getScans()
			cursor?.let {
				val sb = StringBuilder()
				if (cursor.moveToFirst()) {
					val contentIndex = cursor.getColumnIndex(
						Database.SCANS_CONTENT
					)
					do {
						sb.append(cursor.getString(contentIndex))
						sb.append(separator)
					} while (cursor.moveToNext())
				}
				cursor.close()
				val text = sb.toString()
				if (text.isNotEmpty()) {
					GlobalScope.launch(Main) {
						progressView.visibility = View.GONE
						shareText(context, text)
					}
				}
			}
		}
	}
}
