package de.markusfisch.android.binaryeye.fragment

import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
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
import de.markusfisch.android.binaryeye.app.askForFileName
import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.app.saveByteArray
import de.markusfisch.android.binaryeye.app.shareText
import de.markusfisch.android.binaryeye.app.useVisibility
import de.markusfisch.android.binaryeye.data.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
			val cursor = db.getScans()
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

	private fun showScan(id: Long) = db.getScan(id)?.use { cursor ->
		if (cursor.moveToFirst()) {
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
			val scans = getScans {
				withContext(Dispatchers.Main) {
					suspendCoroutine<Boolean?> { continuation ->
						AlertDialog.Builder(context)
							//TODO add res strings
							.setTitle("Allow binary data in output?")
							.setMessage("You probably don't want to allow, that, as it probably breaks the file format.")
							.setPositiveButton(android.R.string.no) { _, _ ->
								continuation.resume(false)
							}
							.setNegativeButton(android.R.string.yes) { _, _ ->
								continuation.resume(true)
							}
							.setNeutralButton(android.R.string.cancel) { _, _ ->
								continuation.resume(null)
							}
							.setOnCancelListener {
								continuation.resume(null)
							}
							.show()
					}
				}
			} ?: return@useVisibility
			val delimiters = context.resources.getStringArray(
				R.array.csv_delimiters_values
			)
			val delimiter = withContext(Dispatchers.Main) {
				suspendCoroutine<String?> { continuation ->
					AlertDialog.Builder(context)
						.setTitle(R.string.csv_delimiter)
						.setItems(R.array.csv_delimiters_names) { _, which ->
							continuation.resume(delimiters[which])
						}
						.setOnCancelListener {
							continuation.resume(null)
						}
						.show()
				}
			} ?: return@useVisibility
			// TODO get other properties, otherwise it doesn't make any sense to call it csv
			val name = withContext(Dispatchers.Main) { activity.askForFileName(suffix = "csv") } ?: return@useVisibility
			val csv = mutableListOf<Byte>()
			for ((content, binaryContent) in scans) {
				if (binaryContent != null) {
					csv.addAll(binaryContent.toList())
				} else {
					csv.addAll(content.toByteArray().toList())
				}
				csv.add('\n'.toByte())
			}
			val toastMessage = saveByteArray(name, csv.toByteArray())
			if (toastMessage > 0) {
				Toast.makeText(
					context,
					toastMessage,
					Toast.LENGTH_SHORT
				).show()
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

	private fun shareScans(separator: String) {
		scope.launch {
			progressView.useVisibility {
				val sb = StringBuilder()
				getScans { false }?.forEach {
					sb.append(it.first)
					sb.append(separator)
				}
				val text = sb.toString()
				if (text.isNotEmpty()) withContext(Dispatchers.Main) {
					shareText(context, text)
				}
			}
		}
	}

	// TODO move this stuff into repo ?!
	@WorkerThread
	private suspend inline fun getScans(crossinline binaryData: suspend () -> Boolean?): List<Pair<String, ByteArray?>>? {
		return db.getScans()?.use { cursor ->
			if (cursor.count <= 0) return@use null
			val getBinaries: Boolean = db.hasBinaryData() && binaryData() ?: return@use null
			val contentIndex = cursor.getColumnIndex(Database.SCANS_CONTENT)
			val idIndex = if (getBinaries) cursor.getColumnIndex(Database.SCANS_ID) else 0
			return cursor.asIterable.mapNotNull {
				val content = cursor.getString(contentIndex)
				return@mapNotNull when {
					content.isNotEmpty() -> content to null
					getBinaries -> db.getScan(cursor.getLong(idIndex))?.use scanUse@{ scanCursor ->
						if (!cursor.moveToFirst()) return@mapNotNull null
						val scanRawIndex = scanCursor.getColumnIndex(Database.SCANS_RAW)
						content to cursor.getBlob(scanRawIndex)
					}
					else -> null
				}
			}
		}
	}
}

private val Cursor.asIterable: Iterable<Cursor>
	get() = object : Iterable<Cursor> {
		override fun iterator(): Iterator<Cursor> = object : Iterator<Cursor> {
			override fun hasNext(): Boolean = position + 1 < count
			override fun next(): Cursor = if (moveToNext()) this@asIterable else throw IllegalStateException("You can't access more items, then there are")
		}
	}
