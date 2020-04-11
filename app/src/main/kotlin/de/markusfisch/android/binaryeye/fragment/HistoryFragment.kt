package de.markusfisch.android.binaryeye.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.WorkerThread
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.SwitchCompat
import android.view.*
import android.widget.EditText
import android.widget.ListView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.ScansAdapter
import de.markusfisch.android.binaryeye.app.*
import de.markusfisch.android.binaryeye.data.Database
import de.markusfisch.android.binaryeye.data.csv.csvBuilder
import de.markusfisch.android.binaryeye.repository.Scan
import de.markusfisch.android.binaryeye.view.setPadding
import de.markusfisch.android.binaryeye.widget.toast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import java.io.IOException

@FlowPreview
@ExperimentalCoroutinesApi
class HistoryFragment : Fragment() {
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
				val ac = activity
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
			return when (item.itemId) {
				R.id.edit_scan -> {
					askForName(
						activity,
						selectedScanId,
						getScanName(selectedScanPosition)
					)
					closeActionMode()
					true
				}
				R.id.remove_scan -> {
					askToRemoveScan(activity, selectedScanId)
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
	private var selectedScanId = 0L
	private var selectedScanPosition = -1

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View {
		activity?.setTitle(R.string.history)

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
		listView.setOnItemLongClickListener { _, v, position, id ->
			v.isSelected = true
			selectedScanId = id
			selectedScanPosition = position
			val ac = activity
			if (actionMode == null && ac is AppCompatActivity) {
				actionMode = ac.delegate.startSupportActionMode(
					actionModeCallback
				)
			}
			true
		}
		listView.setOnScrollListener(systemBarScrollListener)

		fab = view.findViewById(R.id.share)
		fab.setOnClickListener { v ->
			pickListSeparatorAndShare(v.context)
		}

		progressView = view.findViewById(R.id.progress_view)

		setWindowInsetListener { insets ->
			(view.findViewById(R.id.inset_layout) as View).setPadding(insets)
			listView.setPadding(insets)
		}

		return view
	}

	override fun onDestroy() {
		super.onDestroy()
		scansAdapter?.changeCursor(null)
		parentJob.cancel()
	}

	override fun onResume() {
		super.onResume()
		update(context)
	}

	override fun onPause() {
		super.onPause()
		listViewState = listView.onSaveInstanceState()
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
			R.id.export_history -> {
				askToExportToFile(context)
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

	private fun update(context: Context) {
		scope.launch {
			val cursor = db.getScansCursor()
			withContext(Dispatchers.Main) {
				fab.visibility = if (cursor != null && cursor.count > 0) {
					View.VISIBLE
				} else {
					View.GONE
				}
				cursor?.let { cursor ->
					// close previous cursor
					scansAdapter?.also { it.changeCursor(null) }
					scansAdapter = ScansAdapter(context, cursor)
					listView.adapter = scansAdapter
					listViewState?.also {
						listView.onRestoreInstanceState(it)
					}
				}
			}
		}
	}

	private fun closeActionMode() {
		unlockStatusBarColor()
		selectedScanId = 0L
		selectedScanPosition = -1
		actionMode?.finish()
		actionMode = null
		scansAdapter?.notifyDataSetChanged()
	}

	private fun showScan(id: Long) = db.getScan(id)?.also { scan ->
		closeActionMode()
		try {
			addFragment(
				fragmentManager,
				DecodeFragment.newInstance(scan)
			)
		} catch (e: IllegalArgumentException) {
			// ignore, can never happen
		}
	}

	private fun getScanName(position: Int): String? {
		val cursor = scansAdapter?.getItem(position) as Cursor?
		return cursor?.getString(cursor.getColumnIndex(Database.SCANS_NAME))
	}

	// dialogs don't have a parent layout
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
				update(context)
			}
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.show()
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

	private fun askToExportToFile(context: Context) = scope.launch {
		progressView.useVisibility {
			if (!hasWritePermission(activity)) {
				return@useVisibility
			}
			val (getBinaries, scans) = getScans {
				alertDialog(context) { resume ->
					setTitle(R.string.csv_allow_binary)
					setMessage(R.string.csv_allow_binary_message)
					setPositiveButton(R.string.no) { _, _ ->
						resume(false)
					}
					setNegativeButton(android.R.string.yes) { _, _ ->
						resume(true)
					}
					setNeutralButton(android.R.string.cancel) { _, _ ->
						resume(null)
					}
				}
			} ?: return@useVisibility
			val delimiters = context.resources.getStringArray(
				R.array.csv_delimiters_values
			)
			val delimiter = alertDialog<String>(context) { resume ->
				setTitle(R.string.csv_delimiter)
				setItems(R.array.csv_delimiters_names) { _, which ->
					resume(delimiters[which])
				}
			} ?: return@useVisibility
			val name = withContext(Dispatchers.Main) {
				activity.askForFileName(suffix = ".csv")
			} ?: return@useVisibility
			val message = try {
				val out = getExternalOutputStream(
					context,
					name,
					"text/csv"
				)
				out ?: throw IOException()
				val csv = scans.toCSV(delimiter, getBinaries)
				csv.collect {
					withContext(Dispatchers.IO) {
						out.write(it)
					}
				}
				R.string.saved_in_downloads
			} catch (e: FileAlreadyExistsException) {
				R.string.error_file_exists
			} catch (e: IOException) {
				R.string.error_saving_binary_data
			}
			withContext(Dispatchers.Main) {
				context.toast(message)
			}
		}
	}

	private fun Flow<Scan>.toCSV(
		delimiter: String,
		allowBinary: Boolean
	): Flow<ByteArray> {
		return csvBuilder<Scan> {
			column {
				name = "DATE"
				gettingByString { it.timestamp }
			}
			column {
				name = "TYPE"
				gettingByString { it.format }
			}
			column {
				name = "CONTENT"
				gettingByString { it.content }
			}
			if (allowBinary) column {
				isBinary = true
				name = "BINARY_CONTENT"
				gettingBy { it.raw ?: ByteArray(0) }
			}
			column {
				name = "ERROR_CORRECTION_LEVEL"
				gettingByString { it.errorCorrectionLevel }
			}
			column {
				name = "ISSUE_NUMBER"
				gettingByString { it.issueNumber }
			}
			column {
				name = "ORIENTATION"
				gettingByString { it.orientation }
			}
			column {
				name = "OTHER"
				gettingByString { it.otherMetaData }
			}
			column {
				name = "PDF417_EXTRA_METADATA"
				gettingByString { it.pdf417ExtraMetaData }
			}
			column {
				name = "POSSIBLE_COUNTRY"
				gettingByString { it.possibleCountry }
			}
			column {
				name = "SUGGESTED_PRICE"
				gettingByString { it.suggestedPrice }
			}
			column {
				name = "UPC_EAN_EXTENSION"
				gettingByString { it.upcEanExtension }
			}
		}.buildWith(this, delimiter)
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
		scope.launch {
			progressView.useVisibility {
				val sb = StringBuilder()
				getScans { false }?.second?.collect {
					sb.append(it.content)
					sb.append(separator)
				}
				val text = sb.toString()
				if (text.isNotEmpty()) withContext(Dispatchers.Main) {
					shareText(context, text)
				}
			}
		}
	}

	@WorkerThread
	private suspend inline fun getScans(
		crossinline binaryData: suspend () -> Boolean?
	): Pair<Boolean, Flow<Scan>>? {
		val getBinaries: Boolean = db.hasBinaryData() && binaryData() ?: return null
		return getBinaries to db.getScans().mapNotNull {
			when {
				it.content.isNotEmpty() -> db.getScan(it.id)
				getBinaries -> db.getScan(it.id)
				else -> null
			}
		}
	}
}
