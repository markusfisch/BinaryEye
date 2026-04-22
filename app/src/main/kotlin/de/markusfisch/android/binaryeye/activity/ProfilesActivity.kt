package de.markusfisch.android.binaryeye.activity

import android.app.AlertDialog
import android.content.Context
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
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat

class ProfilesActivity : AbstractBaseActivity() {
	private val profiles = ArrayList<ProfileItem>()

	private lateinit var adapter: ProfilesAdapter

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setScreenContentView(R.layout.activity_profiles)
		setTitle(R.string.profile)

		refreshProfiles()

		val listView = findViewById<ListView>(R.id.profiles)
		listView.emptyView = findViewById(R.id.no_profiles)
		adapter = ProfilesAdapter(this, profiles)
		listView.adapter = adapter
		listView.setOnScrollListener(systemBarListViewScrollListener)
		listView.setOnItemClickListener { _, _, position, _ ->
			selectProfile(position)
		}
		listView.setOnItemLongClickListener { _, _, position, _ ->
			confirmRemoveProfile(position)
			true
		}

		findViewById<View>(R.id.add).setOnClickListener {
			addProfile()
		}

		findViewById<View>(R.id.inset_layout).setPaddingFromWindowInsets()
		listView.setPaddingFromWindowInsets()
	}

	private fun refreshProfiles() {
		profiles.clear()
		profiles.add(
			ProfileItem(
				null,
				getString(R.string.profile_default)
			)
		)
		prefs.profiles.forEach { name ->
			profiles.add(ProfileItem(name, name))
		}
		if (this::adapter.isInitialized) {
			adapter.notifyDataSetChanged()
		}
	}

	private fun selectProfile(position: Int) {
		val profile = profiles[position].name
		if (prefs.profile == profile) {
			return
		}
		prefs.load(this, profile)
		adapter.notifyDataSetChanged()
	}

	// Dialogs do not have a parent view.
	@Suppress("InflateParams")
	private fun addProfile() {
		val view = layoutInflater.inflate(
			R.layout.dialog_profile_name, null
		)
		val editText = view.findViewById<EditText>(R.id.name)
		AlertDialog.Builder(this)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val name = editText.text.toString().trim()
				if (name.isNotEmpty() && prefs.addProfile(name)) {
					prefs.load(this, name)
					refreshProfiles()
				} else {
					toast(R.string.profile_invalid_name)
				}
			}
			.show()
	}

	private fun confirmRemoveProfile(position: Int) {
		val profile = profiles[position]
		val profileName = profile.name ?: return
		AlertDialog.Builder(this)
			.setMessage(R.string.profile_remove_confirm)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val isCurrent = prefs.profile == profileName
				prefs.removeProfile(profileName)
				if (isCurrent) {
					prefs.load(this, null)
				}
				refreshProfiles()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ -> }
			.show()
	}

	private class ProfilesAdapter(
		context: Context,
		private val items: List<ProfileItem>
	) : ArrayAdapter<ProfileItem>(
		context,
		R.layout.row_profile,
		R.id.title,
		items
	) {
		override fun getView(
			position: Int,
			convertView: View?,
			parent: ViewGroup
		): View {
			val view = super.getView(position, convertView, parent)
			val item = items[position]
			val isCurrent = item.name == prefs.profile
			getViewHolder(view).apply {
				title.text = item.label
				if (isCurrent) {
					subtitle.text = context.getString(R.string.profile_current)
					subtitle.visibility = View.VISIBLE
				} else {
					subtitle.text = ""
					subtitle.visibility = View.GONE
				}
				shareQr.setOnClickListener {
					context.startActivity(
						BarcodeActivity.newIntent(
							context,
							prefs.toJson(context, item.name),
							BarcodeFormat.QRCode,
							addQuietZone = true
						)
					)
				}
			}
			return view
		}

		private fun getViewHolder(
			view: View
		): ViewHolder = view.tag as ViewHolder? ?: ViewHolder(
			view.findViewById(R.id.title),
			view.findViewById(R.id.subtitle),
			view.findViewById(R.id.share_qr),
		).also {
			view.tag = it
		}

		private data class ViewHolder(
			val title: TextView,
			val subtitle: TextView,
			val shareQr: View,
		)
	}
}

private data class ProfileItem(
	val name: String?,
	val label: String
)
