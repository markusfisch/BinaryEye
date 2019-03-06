package de.markusfisch.android.binaryeye.fragment

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.adapter.ScansAdapter
import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.data.Database
import de.markusfisch.android.binaryeye.fragment.DecodeFragment
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

		fab = view.findViewById(R.id.clear)
		fab.setOnClickListener { v ->
			askToRemoveAllScans(v.context)
		}

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

	private fun initHistorySwitch(switchView: SwitchCompat) {
		switchView.setOnCheckedChangeListener { _, isChecked ->
			prefs.useHistory = isChecked
		}
		if (prefs.useHistory) {
			switchView.toggle()
		}
	}

	private fun update(context: Context) {
		GlobalScope.launch {
			val cursor = db.getScans()
			GlobalScope.launch(Main) {
				fab.visibility = if (cursor != null && cursor.getCount() > 0) {
					View.VISIBLE
				} else {
					View.GONE
				}
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

	private fun showScan(id: Long) {
		val cursor = db.getScan(id)
		if (cursor != null && cursor.moveToFirst()) {
			val contentIndex = cursor.getColumnIndex(Database.SCANS_CONTENT)
			val formatIndex = cursor.getColumnIndex(Database.SCANS_FORMAT)
			try {
				addFragment(
					fragmentManager,
					DecodeFragment.newInstance(
						cursor.getString(contentIndex),
						BarcodeFormat.valueOf(cursor.getString(formatIndex))
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
}
