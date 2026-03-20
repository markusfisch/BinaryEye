package de.markusfisch.android.binaryeye.activity

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.annotation.RequiresApi
import androidx.core.util.size
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.view.systemBarListViewScrollListener
import de.markusfisch.android.binaryeye.widget.toast

class NetworkSuggestionsActivity : ScreenActivity() {
	@RequiresApi(Build.VERSION_CODES.R)
	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setTitle(R.string.network_suggestions)
		val frame = findViewById(R.id.content_frame) as ViewGroup
		val view = layoutInflater.inflate(
			R.layout.fragment_network_suggestions,
			frame,
			false
		)
		frame.addView(view)

		val wm = applicationContext.getSystemService(
			Context.WIFI_SERVICE
		) as WifiManager
		val suggestionArrayAdapter = ArrayAdapter(
			this,
			android.R.layout.simple_list_item_checked,
			wm.networkSuggestions.map {
				Suggestion(
					it.ssid ?: it.toString(),
					it
				)
			}
		)

		val listView = view.findViewById<ListView>(R.id.suggestions)
		listView.emptyView = view.findViewById(R.id.no_suggestions)
		listView.adapter = suggestionArrayAdapter
		listView.setOnScrollListener(systemBarListViewScrollListener)
		view.findViewById<View>(R.id.inset_layout).setPaddingFromWindowInsets()
		listView.setPaddingFromWindowInsets()

		view.findViewById<View>(R.id.remove).setOnClickListener {
			val removeList = ArrayList<WifiNetworkSuggestion>()
			val removeFromAdapter = ArrayList<Suggestion>()
			val checked = listView.checkedItemPositions
			val size = checked.size
			for (i in 0 until size) {
				if (checked.valueAt(i)) {
					val pos = checked.keyAt(i)
					listView.setItemChecked(pos, false)
					val suggestion = listView.getItemAtPosition(
						pos
					) as Suggestion
					removeList.add(suggestion.suggestion)
					removeFromAdapter.add(suggestion)
				}
			}
			if (removeList.isNotEmpty()) {
				removeFromAdapter.forEach {
					suggestionArrayAdapter.remove(it)
				}
				wm.removeNetworkSuggestions(removeList)
			} else {
				toast(
					R.string.clear_network_suggestions_nothing_to_remove
				)
			}
		}
	}
}

private data class Suggestion(
	val label: String,
	val suggestion: WifiNetworkSuggestion
) {
	override fun toString() = label
}
