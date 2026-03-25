package de.markusfisch.android.binaryeye.activity

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.adapter.setupFormatSpinner
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.preference.IgnoreCode
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.view.systemBarListViewScrollListener
import de.markusfisch.android.binaryeye.widget.toast

class IgnoreCodesActivity : ScreenActivity() {
	private val patterns = ArrayList<IgnoreCode>()

	private lateinit var adapter: PatternsAdapter

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setTitle(R.string.ignore_codes)
		val frame = findViewById(R.id.content_frame) as ViewGroup
		val view = layoutInflater.inflate(
			R.layout.fragment_automated_actions,
			frame,
			false
		)
		frame.addView(view)

		patterns.addAll(prefs.ignoreCodes)

		val listView = view.findViewById<ListView>(R.id.actions)
		listView.emptyView = view.findViewById(R.id.no_actions)
		view.findViewById<TextView>(R.id.no_actions).setText(
			R.string.ignore_codes_empty
		)
		adapter = PatternsAdapter(patterns)
		listView.adapter = adapter
		listView.setOnScrollListener(systemBarListViewScrollListener)
		listView.setOnItemClickListener { _, _, position, _ ->
			editPattern(position)
		}
		listView.setOnItemLongClickListener { _, _, position, _ ->
			confirmRemovePattern(position)
			true
		}

		view.findViewById<View>(R.id.add).apply {
			contentDescription = getString(R.string.ignore_code_add)
			setOnClickListener {
				editPattern(null)
			}
		}

		view.findViewById<View>(R.id.inset_layout).setPaddingFromWindowInsets()
		listView.setPaddingFromWindowInsets()
	}

	private fun editPattern(position: Int?) {
		val pattern = position?.let { patterns[it] }
		val view = layoutInflater.inflate(R.layout.dialog_ignore_code, null)
		val regexView = view.findViewById<EditText>(R.id.regex)
		val formatView = view.findViewById<Spinner>(R.id.format)
		val values = setupFormatSpinner(formatView)
		if (pattern != null) {
			regexView.setText(pattern.pattern)
			regexView.setSelection(pattern.pattern.length)
			formatView.setSelection(
				values.indexOf(pattern.format ?: "").coerceAtLeast(0)
			)
		} else {
			formatView.setSelection(0)
		}

		val dialog = AlertDialog.Builder(this)
			.setTitle(
				if (pattern == null) {
					R.string.ignore_code_add
				} else {
					R.string.ignore_code_edit
				}
			)
			.setView(view)
			.setPositiveButton(android.R.string.ok, null)
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.create()

		dialog.setOnShowListener {
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
				addOrUpdatePattern(position, regexView, formatView, values, dialog)
			}
		}

		dialog.show()
	}

	private fun addOrUpdatePattern(
		position: Int?,
		regexView: EditText,
		formatView: Spinner,
		values: List<String>,
		dialog: Dialog,
	) {
		val pattern = regexView.text.toString().trim()
		if (pattern.isEmpty()) {
			regexView.error = getString(R.string.ignore_code_regex_required)
			return
		}
		try {
			Regex(pattern)
		} catch (e: Exception) {
			val message = e.message ?: getString(
				R.string.ignore_code_regex_invalid
			)
			regexView.error = message
			toast(message)
			return
		}
		val item = IgnoreCode(
			pattern = pattern,
			format = values[formatView.selectedItemPosition].ifEmpty { null }
		)
		if (position == null) {
			patterns.add(item)
		} else {
			patterns[position] = item
		}
		prefs.setIgnoreCodes(patterns)
		adapter.notifyDataSetChanged()
		dialog.dismiss()
	}

	private fun confirmRemovePattern(position: Int) {
		AlertDialog.Builder(this)
			.setMessage(R.string.ignore_code_remove_confirm)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				patterns.removeAt(position)
				prefs.setIgnoreCodes(patterns)
				adapter.notifyDataSetChanged()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.show()
	}

	private inner class PatternsAdapter(
		items: List<IgnoreCode>
	) : ArrayAdapter<IgnoreCode>(
		this,
		android.R.layout.simple_list_item_2,
		android.R.id.text1,
		items
	) {
		override fun getView(
			position: Int,
			convertView: View?,
			parent: ViewGroup
		): View {
			val view = super.getView(position, convertView, parent)
			val item = patterns[position]
			val title = view.findViewById<TextView>(android.R.id.text1)
			val subtitle = view.findViewById<TextView>(android.R.id.text2)
			title.text = item.pattern
			subtitle.text = item.format?.prettifyFormatName() ?: getString(
				R.string.all_formats
			)
			return view
		}
	}
}
