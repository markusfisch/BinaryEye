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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.ScansAdapter
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.alertDialog
import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.content.copyToClipboard
import de.markusfisch.android.binaryeye.content.shareAsFile
import de.markusfisch.android.binaryeye.content.shareText
import de.markusfisch.android.binaryeye.content.wipeLastShareFile
import de.markusfisch.android.binaryeye.database.Database
import de.markusfisch.android.binaryeye.database.exportCsv
import de.markusfisch.android.binaryeye.database.exportDatabase
import de.markusfisch.android.binaryeye.database.exportJson
import de.markusfisch.android.binaryeye.database.use
import de.markusfisch.android.binaryeye.io.askForFileName
import de.markusfisch.android.binaryeye.io.toSaveResult
import de.markusfisch.android.binaryeye.view.lockStatusBarColor
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.view.systemBarListViewScrollListener
import de.markusfisch.android.binaryeye.view.unlockStatusBarColor
import de.markusfisch.android.binaryeye.view.useVisibility
import de.markusfisch.android.binaryeye.widget.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
					scansAdapter?.getSelectedContent("\n")?.let {
						ac.copyToClipboard(it)
						ac.toast(R.string.copied_to_clipboard)
					}
					closeActionMode()
					true
				}

				R.id.edit_label -> {
					scansAdapter?.forSelection { id, position ->
						ac.askForName(
							id,
							scansAdapter?.getName(position),
							scansAdapter?.getContent(position)
						)
					}
					closeActionMode()
					true
				}

				R.id.remove_scan -> {
					scansAdapter?.getSelectedIds()?.let {
						if (it.isNotEmpty()) {
							ac.askToRemoveScans(it)
						}
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
	private var shareAsFileMenuItem: MenuItem? = null

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
		listView.setOnItemClickListener { _, v, position, id ->
			if (actionMode != null) {
				scansAdapter?.select(v, id, position)
			} else {
				showScan(id)
			}
		}
		listView.setOnItemLongClickListener { _, v, position, id ->
			scansAdapter?.select(v, id, position)
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
			v.context.pickListSeparatorAndShare(false)
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
		wipeLastShareFile()
	}

	override fun onPause() {
		super.onPause()
		listViewState = listView.onSaveInstanceState()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_history, menu)
		val searchMenuItem = menu.findItem(R.id.search)
		initSearchView(searchMenuItem)
		val visible = scansAdapter?.count != 0
		searchMenuItem?.isVisible = visible
		exportHistoryMenuItem = menu.findItem(R.id.export_history)?.apply {
			isVisible = visible
		}
		shareAsFileMenuItem = menu.findItem(R.id.share_as_file)?.apply {
			isVisible = visible
		}
		clearListMenuItem = menu.findItem(R.id.clear)?.apply {
			isVisible = visible
		}
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
				context.askToRemoveScans()
				true
			}

			R.id.export_history -> {
				askToExportToFile()
				true
			}

			R.id.share_as_file -> {
				context.pickListSeparatorAndShare(true)
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
		shareAsFileMenuItem?.isEnabled = enabled
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

	// Dialogs don't have a parent layout.
	@SuppressLint("InflateParams")
	private fun Context.askForName(id: Long, text: String?, content: String?) {
		val view = LayoutInflater.from(this).inflate(
			R.layout.dialog_enter_name, null
		)
		val nameView = view.findViewById<EditText>(R.id.name)
		nameView.setText(text)
		AlertDialog.Builder(this)
			.setTitle(
				if (content.isNullOrEmpty()) {
					getString(R.string.binary_data)
				} else {
					content
				}
			)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val name = nameView.text.toString()
				db.renameScan(id, name)
				update()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.show()
	}

	private fun Context.askToRemoveScans(ids: List<Long>) {
		AlertDialog.Builder(this)
			.setMessage(
				if (ids.size > 1) {
					R.string.really_remove_selected_scans
				} else {
					R.string.really_remove_scan
				}
			)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				ids.forEach { db.removeScan(it) }
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

	private fun Context.askToRemoveScans() {
		AlertDialog.Builder(this)
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

	private fun Context.pickListSeparatorAndShare(asFile: Boolean) {
		val separators = resources.getStringArray(
			R.array.list_separators_values
		)
		AlertDialog.Builder(this)
			.setTitle(R.string.pick_list_separator)
			.setItems(R.array.list_separators_names) { _, which ->
				shareScans(separators[which], asFile)
			}
			.show()
	}

	private fun shareScans(format: String, asFile: Boolean) = scope.launch {
		progressView.useVisibility {
			val selectedIds = scansAdapter?.getSelectedIds()
			if (selectedIds?.isNotEmpty() == true) {
				db.getScansDetailed(selectedIds.toLongArray())
			} else {
				db.getScansDetailed(filter)
			}?.use { cursor ->
				val details = format.split(":")
				when (details[0]) {
					"text" -> Pair(cursor.exportText(details[1]), "txt")
					"csv" -> Pair(cursor.exportCsv(details[1]), "csv")
					else -> Pair(cursor.exportJson(), "json")
				}
			}?.let { (text, ext) ->
				text?.let {
					withContext(Dispatchers.Main) {
						if (asFile) {
							context.shareAsFile(
								it,
								String.format("scans.%s", ext)
							)
						} else {
							context.shareText(it)
						}
					}
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
