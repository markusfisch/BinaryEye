package de.markusfisch.android.binaryeye.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.annotation.WorkerThread
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.ScansAdapter
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.alertDialog
import de.markusfisch.android.binaryeye.app.askForFileName
import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.app.saveByteArray
import de.markusfisch.android.binaryeye.app.shareText
import de.markusfisch.android.binaryeye.app.useVisibility
import de.markusfisch.android.binaryeye.data.csv.csvBuilder
import de.markusfisch.android.binaryeye.repository.DatabaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment() {
	private lateinit var listView: ListView
	private lateinit var fab: View
	private lateinit var progressView: View

	private val parentJob = Job()
	private val scope = CoroutineScope(Dispatchers.IO + parentJob)

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
		parentJob.cancel()
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

	private fun showScan(id: Long) = db.getScan(id)?.also { scan ->
		try {
			addFragment(
				fragmentManager,
				DecodeFragment.newInstance(
					scan.content,
					BarcodeFormat.valueOf(scan.format),
					scan.raw
				)
			)
		} catch (e: IllegalArgumentException) {
			// shouldn't ever happen
		}
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
			if (!hasWritePermission(activity)) return@useVisibility
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
			val name = withContext(Dispatchers.Main) { activity.askForFileName(suffix = "csv") }
				?: return@useVisibility
			val csv = scans.toCSV(delimiter, getBinaries)
			val toastMessage = saveByteArray(name, csv)
			if (toastMessage > 0) {
				withContext(Dispatchers.Main) {
					Toast.makeText(
						context,
						toastMessage,
						Toast.LENGTH_SHORT
					).show()
				}
			}
		}
	}

	private fun List<DatabaseRepository.Scan>.toCSV(
		delimiter: String,
		allowBinary: Boolean
	): ByteArray {
		return csvBuilder<DatabaseRepository.Scan> {
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
				getScans { false }?.second?.forEach {
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
	private suspend inline fun getScans(crossinline binaryData: suspend () -> Boolean?): Pair<Boolean, List<DatabaseRepository.Scan>>? {
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
