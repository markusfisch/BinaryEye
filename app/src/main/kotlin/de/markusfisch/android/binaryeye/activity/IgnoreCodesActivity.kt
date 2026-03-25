package de.markusfisch.android.binaryeye.activity

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.view.systemBarListViewScrollListener
import de.markusfisch.android.binaryeye.widget.toast

class IgnoreCodesActivity : ScreenActivity() {
	private val patterns = ArrayList<String>()

	private lateinit var adapter: ArrayAdapter<String>

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
		adapter = ArrayAdapter(
			this,
			android.R.layout.simple_list_item_1,
			android.R.id.text1,
			patterns
		)
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
		if (pattern != null) {
			regexView.setText(pattern)
			regexView.setSelection(pattern.length)
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
				addOrUpdatePattern(position, regexView, dialog)
			}
		}

		dialog.show()
	}

	private fun addOrUpdatePattern(
		position: Int?,
		regexView: EditText,
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
		if (position == null) {
			patterns.add(pattern)
		} else {
			patterns[position] = pattern
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
}
