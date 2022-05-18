package de.markusfisch.android.binaryeye.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.SearchView
import android.support.v7.widget.SwitchCompat
import android.view.*
import android.widget.EditText
import android.widget.ListView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.ScansAdapter
import de.markusfisch.android.binaryeye.app.*
import de.markusfisch.android.binaryeye.content.copyToClipboard
import de.markusfisch.android.binaryeye.content.shareText
import de.markusfisch.android.binaryeye.database.Database
import de.markusfisch.android.binaryeye.database.getString
import de.markusfisch.android.binaryeye.database.exportCsv
import de.markusfisch.android.binaryeye.database.exportDatabase
import de.markusfisch.android.binaryeye.database.exportJson
import de.markusfisch.android.binaryeye.io.askForFileName
import de.markusfisch.android.binaryeye.io.toSaveResult
import de.markusfisch.android.binaryeye.view.*
import de.markusfisch.android.binaryeye.widget.toast
import kotlinx.coroutines.*

class HistoryFragment : Fragment() {
	private lateinit var useHistorySwitch: SwitchCompat
	private lateinit var listView: ListView
	private lateinit var fab: View
	private lateinit var progressView: View

	private val parentJob = Job()
	private val scope = CoroutineScope(Dispatchers.IO + parentJob)
	private val actionModeCallback = object : ActionMode.Callback {
		override fun onCreateActionMode(
			mode: ActionMode,
			menu: Menu
		): Boolean {
			mode.menuInflater.inflate(
				R.menu.fragment_history_edit,
				menu
			)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				lockStatusBarColor()
				val ac = activity ?: return false
				ac.window.statusBarColor = ContextCompat.getColor(
					ac,
					R.color.accent_dark
				)
			}
			return true
		}

		override fun onPrepareActionMode(
			mode: ActionMode,
			menu: Menu
		): Boolean {
			return false
		}

		override fun onActionItemClicked(
			mode: ActionMode,
			item: MenuItem
		): Boolean {
			val ac = activity ?: return false
			return when (item.itemId) {
				R.id.copy_scan -> {
					scansAdapter?.getSelectedContent()?.let {
						ac.copyToClipboard(it)
						ac.toast(R.string.copied_to_clipboard)
					}
					closeActionMode()
					true
				}
				R.id.edit_scan -> {
					scansAdapter?.let {
						askForName(
							ac,
							it.selectedScanId,
							getScanName(it.selectedScanPosition)
						)
					}
					closeActionMode()
					true
				}
				R.id.remove_scan -> {
					scansAdapter?.let {
						askToRemoveScan(ac, it.selectedScanId)
					}
					closeActionMode()
					true
				}
				else -> false
			}
		}

		override fun onDestroyActionMode(mode: ActionMode) {
			closeActionMode()
		}
	}

	private var scansAdapter: ScansAdapter? = null
	private var listViewState: Parcelable? = null
	private var actionMode: ActionMode? = null
	private var filter: String? = null
	private var clearListMenuItem: MenuItem? = null
	private var exportHistoryMenuItem: MenuItem? = null

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		val ac = activity ?: return null
		ac.setTitle(R.string.history)

		val view = inflater.inflate(
			R.layout.fragment_history,
			container,
			false
		)

		useHistorySwitch = view.findViewById(
			R.id.use_history
		) as SwitchCompat
		initHistorySwitch(useHistorySwitch)

		listView = view.findViewById(R.id.scans)
		listView.setOnItemClickListener { _, _, _, id ->
			showScan(id)
		}
		listView.setOnItemLongClickListener { _, v, position, id ->
			v.isSelected = true
			scansAdapter?.select(id, position)
			if (actionMode == null && ac is AppCompatActivity) {
				actionMode = ac.delegate.startSupportActionMode(
					actionModeCallback
				)
			}
			true
		}
		listView.setOnScrollListener(systemBarListViewScrollListener)

		fab = view.findViewById(R.id.share)
		fab.setOnClickListener { v ->
			pickListSeparatorAndShare(v.context)
		}

		progressView = view.findViewById(R.id.progress_view)

		(view.findViewById(R.id.inset_layout) as View).setPaddingFromWindowInsets()
		listView.setPaddingFromWindowInsets()

		update()

		return view
	}

	override fun onDestroy() {
		super.onDestroy()
		scansAdapter?.changeCursor(null)
		parentJob.cancel()
	}

	override fun onPause() {
		super.onPause()
		listViewState = listView.onSaveInstanceState()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_history, menu)
		initSearchView(menu.findItem(R.id.search))
		menu.setGroupVisible(R.id.scans_available, scansAdapter?.count != 0)
		clearListMenuItem = menu.findItem(R.id.clear)
		exportHistoryMenuItem = menu.findItem(R.id.export_history)
	}

	private fun initSearchView(item: MenuItem?) {
		item ?: return
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			item.isVisible = false
		} else {
			val ac = activity ?: return
			val searchView = item.actionView as SearchView
			val searchManager = ac.getSystemService(
				Context.SEARCH_SERVICE
			) as SearchManager
			searchView.setSearchableInfo(
				searchManager.getSearchableInfo(ac.componentName)
			)
			searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
				override fun onQueryTextSubmit(query: String): Boolean {
					update(query)
					return false
				}

				override fun onQueryTextChange(query: String): Boolean {
					update(query)
					return false
				}
			})
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.clear -> {
				askToRemoveScans(context)
				true
			}
			R.id.export_history -> {
				askToExportToFile()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun initHistorySwitch(switchView: SwitchCompat) {
		switchView.setOnCheckedChangeListener { _, isChecked ->
			prefs.useHistory = isChecked
		}
		if (prefs.useHistory) {
			switchView.toggle()
		}
	}

	private fun updateAndClearFilter() {
		filter = null
		update()
	}

	private fun update(query: String? = null) {
		query?.let { filter = it }
		scope.launch {
			val cursor = db.getScans(filter)
			withContext(Dispatchers.Main) {
				val ac = activity ?: return@withContext
				val hasScans = cursor != null && cursor.count > 0
				if (filter == null) {
					if (!hasScans) {
						listView.emptyView = useHistorySwitch
					}
					ActivityCompat.invalidateOptionsMenu(ac)
				}
				enableMenuItems(hasScans)
				fab.visibility = if (hasScans) {
					View.VISIBLE
				} else {
					View.GONE
				}
				cursor?.let { cursor ->
					// Close previous cursor.
					scansAdapter?.also { it.changeCursor(null) }
					scansAdapter = ScansAdapter(ac, cursor)
					listView.adapter = scansAdapter
					listViewState?.also {
						listView.onRestoreInstanceState(it)
					}
				}
			}
		}
	}

	private fun enableMenuItems(enabled: Boolean) {
		clearListMenuItem?.isEnabled = enabled
		exportHistoryMenuItem?.isEnabled = enabled
	}

	private fun closeActionMode() {
		unlockStatusBarColor()
		scansAdapter?.clearSelection()
		actionMode?.finish()
		actionMode = null
		scansAdapter?.notifyDataSetChanged()
	}

	private fun showScan(id: Long) = db.getScan(id)?.also { scan ->
		closeActionMode()
		try {
			fragmentManager?.addFragment(DecodeFragment.newInstance(scan))
		} catch (e: IllegalArgumentException) {
			// Ignore, can never happen.
		}
	}

	private fun getScanName(position: Int): String? {
		val cursor = scansAdapter?.getItem(position) as Cursor?
		return cursor?.getString(Database.SCANS_NAME)
	}

	// Dialogs don't have a parent layout.
	@SuppressLint("InflateParams")
	private fun askForName(context: Context, id: Long, text: String?) {
		val view = LayoutInflater.from(context).inflate(
			R.layout.dialog_enter_name, null
		)
		val nameView = view.findViewById<EditText>(R.id.name)
		nameView.setText(text)
		AlertDialog.Builder(context)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val name = nameView.text.toString()
				db.renameScan(id, name)
				update()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.show()
	}

	private fun askToRemoveScan(context: Context, id: Long) {
		AlertDialog.Builder(context)
			.setMessage(R.string.really_remove_scan)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				db.removeScan(id)
				if (scansAdapter?.count == 1) {
					updateAndClearFilter()
				} else {
					update()
				}
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
	}

	private fun askToRemoveScans(context: Context) {
		AlertDialog.Builder(context)
			.setMessage(
				if (filter == null) {
					R.string.really_remove_all_scans
				} else {
					R.string.really_remove_selected_scans
				}
			)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				db.removeScans(filter)
				updateAndClearFilter()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
	}

	private fun askToExportToFile() {
		scope.launch {
			val ac = activity ?: return@launch
			progressView.useVisibility {
				// Write permission is only required before Android Q.
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
					!ac.hasWritePermission { askToExportToFile() }
				) {
					return@useVisibility
				}
				val options = ac.resources.getStringArray(
					R.array.export_options_values
				)
				val delimiter = alertDialog<String>(ac) { resume ->
					setTitle(R.string.export_as)
					setItems(R.array.export_options_names) { _, which ->
						resume(options[which])
					}
				} ?: return@useVisibility
				val name = withContext(Dispatchers.Main) {
					ac.askForFileName(
						when (delimiter) {
							"db" -> ".db"
							"json" -> ".json"
							else -> ".csv"
						}
					)
				} ?: return@useVisibility
				val message = when (delimiter) {
					"db" -> ac.exportDatabase(name)
					else -> db.getScansDetailed(filter)?.use {
						when (delimiter) {
							"json" -> ac.exportJson(name, it)
							else -> ac.exportCsv(name, it, delimiter)
						}
					} ?: false
				}.toSaveResult()
				withContext(Dispatchers.Main) {
					ac.toast(message)
				}
			}
		}
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

	private fun shareScans(format: String) = scope.launch {
		progressView.useVisibility {
			var text: String? = null
			db.getScansDetailed(filter)?.use { cursor ->
				val details = format.split(":")
				text = when (details[0]) {
					"text" -> cursor.exportText(details[1])
					"csv" -> cursor.exportCsv(details[1])
					else -> cursor.exportJson()
				}
			}
			text?.let {
				withContext(Dispatchers.Main) {
					context.shareText(it)
				}
			}
		}
	}
}

private fun Cursor.exportText(separator: String): String {
	val sb = StringBuilder()
	val contentIndex = getColumnIndex(Database.SCANS_CONTENT)
	if (contentIndex > -1 && moveToFirst()) {
		do {
			val content = getString(contentIndex)
			if (content?.isNotEmpty() == true) {
				sb.append(content)
				sb.append(separator)
			}
		} while (moveToNext())
	}
	return sb.toString()
}
