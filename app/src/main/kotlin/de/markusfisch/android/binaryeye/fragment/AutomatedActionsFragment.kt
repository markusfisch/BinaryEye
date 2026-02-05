package de.markusfisch.android.binaryeye.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.automation.AutomatedAction
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.view.systemBarListViewScrollListener

class AutomatedActionsFragment : Fragment() {
	private val actions = ArrayList<AutomatedAction>()

	private lateinit var adapter: ActionsAdapter

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		val ac = activity ?: return null
		ac.setTitle(R.string.automated_actions)

		val view = inflater.inflate(
			R.layout.fragment_automated_actions,
			container,
			false
		)

		actions.clear()
		actions.addAll(prefs.automatedActions)

		val listView = view.findViewById<ListView>(R.id.actions)
		listView.emptyView = view.findViewById(R.id.no_actions)
		adapter = ActionsAdapter(ac, actions)
		listView.adapter = adapter
		listView.setOnScrollListener(systemBarListViewScrollListener)
		listView.setOnItemClickListener { _, _, position, _ ->
			editAction(position)
		}
		listView.setOnItemLongClickListener { _, _, position, _ ->
			confirmRemoveAction(position)
			true
		}

		view.findViewById<View>(R.id.add).setOnClickListener {
			editAction(null)
		}

		view.findViewById<View>(R.id.inset_layout).setPaddingFromWindowInsets()
		listView.setPaddingFromWindowInsets()

		return view
	}

	private fun editAction(position: Int?) {
		val ac = activity ?: return
		val action = position?.let { actions[it] }
		val view = ac.layoutInflater.inflate(
			R.layout.dialog_automated_action,
			null
		)
		val regexView = view.findViewById<EditText>(R.id.regex)
		val urlView = view.findViewById<EditText>(R.id.url_template)
		val typeGroup = view.findViewById<RadioGroup>(R.id.action_type)
		val urlSection = view.findViewById<View>(R.id.url_section)
		val intentSection = view.findViewById<View>(R.id.intent_section)
		val intentUriView = view.findViewById<EditText>(
			R.id.intent_uri_template
		)

		if (action != null) {
			regexView.setText(action.pattern)
			when (action.type) {
				AutomatedAction.Type.Intent -> {
					urlView.setText(action.urlTemplate ?: "")
				}

				AutomatedAction.Type.CustomIntent -> {
					intentUriView.setText(action.intentUriTemplate ?: "")
				}
			}
		}

		fun updateTypeSections(checkedId: Int) {
			val isIntent = checkedId == R.id.action_type_intent
			urlSection.visibility = if (isIntent) View.VISIBLE else View.GONE
			intentSection.visibility = if (isIntent) View.GONE else View.VISIBLE
		}

		typeGroup.setOnCheckedChangeListener { _, checkedId ->
			updateTypeSections(checkedId)
		}
		val initialTypeId = if (
			action?.type == AutomatedAction.Type.CustomIntent
		) {
			R.id.action_type_custom_intent
		} else {
			R.id.action_type_intent
		}
		typeGroup.check(initialTypeId)
		updateTypeSections(initialTypeId)

		val dialog = AlertDialog.Builder(ac)
			.setTitle(
				if (action == null) {
					R.string.automated_action_add
				} else {
					R.string.automated_action_edit
				}
			)
			.setView(view)
			.setPositiveButton(android.R.string.ok, null)
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.create()

		dialog.setOnShowListener {
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
				addOrUpdateAction(
					position,
					regexView,
					urlView,
					typeGroup,
					intentUriView,
					dialog
				)
			}
		}

		dialog.show()
	}

	private fun addOrUpdateAction(
		position: Int?,
		regexView: EditText,
		urlView: EditText,
		typeGroup: RadioGroup,
		intentUriView: EditText,
		dialog: Dialog,
	) {
		val pattern = regexView.text.toString().trim()
		if (pattern.isEmpty()) {
			regexView.error = getString(R.string.automated_action_regex_required)
			return
		}
		try {
			Regex(pattern)
		} catch (_: Exception) {
			regexView.error = getString(R.string.automated_action_regex_invalid)
			return
		}
		val isIntent = typeGroup.checkedRadioButtonId == R.id.action_type_intent
		val newAction = if (isIntent) {
			val urlTemplate = urlView.text.toString().trim()
			if (urlTemplate.isEmpty()) {
				urlView.error = getString(R.string.automated_action_url_required)
				return
			}
			AutomatedAction(
				pattern = pattern,
				type = AutomatedAction.Type.Intent,
				urlTemplate = urlTemplate
			)
		} else {
			val intentUriTemplate = intentUriView.text.toString().trim()
			if (intentUriTemplate.isEmpty()) {
				intentUriView.error = getString(
					R.string.automated_action_intent_uri_required
				)
				return
			}
			if (!intentUriTemplate.startsWith(
					"intent:",
					ignoreCase = true
				)
			) {
				intentUriView.error = getString(
					R.string.automated_action_intent_uri_invalid
				)
				return
			}
			AutomatedAction(
				pattern = pattern,
				type = AutomatedAction.Type.CustomIntent,
				intentUriTemplate = intentUriTemplate
			)
		}
		if (position == null) {
			actions.add(newAction)
		} else {
			actions[position] = newAction
		}
		prefs.setAutomatedActions(actions)
		adapter.notifyDataSetChanged()
		dialog.dismiss()
	}

	private fun confirmRemoveAction(position: Int) {
		val ac = activity ?: return
		AlertDialog.Builder(ac)
			.setMessage(R.string.automated_action_remove_confirm)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				actions.removeAt(position)
				prefs.setAutomatedActions(actions)
				adapter.notifyDataSetChanged()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.show()
	}

	private class ActionsAdapter(
		context: Context,
		private val items: List<AutomatedAction>
	) : ArrayAdapter<AutomatedAction>(
		context,
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
			val action = items[position]
			getViewHolder(view).apply {
				title.text = action.pattern
				subtitle.text = when (action.type) {
					AutomatedAction.Type.Intent -> if (!action.urlTemplate.isNullOrEmpty()) {
						context.getString(
							R.string.automated_action_list_intent,
							action.urlTemplate
						)
					} else {
						context.getString(R.string.automated_action_list_intent_empty)
					}

					AutomatedAction.Type.CustomIntent ->
						if (!action.intentUriTemplate.isNullOrEmpty()) {
							context.getString(
								R.string.automated_action_list_custom_intent,
								action.intentUriTemplate
							)
						} else {
							context.getString(
								R.string.automated_action_list_custom_intent_empty
							)
						}
				}
			}
			return view
		}

		private fun getViewHolder(
			view: View
		): ViewHolder = view.tag as ViewHolder? ?: ViewHolder(
			view.findViewById(android.R.id.text1),
			view.findViewById(android.R.id.text2),
		).also {
			view.tag = it
		}

		private data class ViewHolder(
			val title: TextView,
			val subtitle: TextView,
		)
	}
}
