package de.markusfisch.android.binaryeye.fragment

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.R

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.ClipboardManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class DecodeFragment : Fragment() {
	private lateinit var contentView: EditText
	private lateinit var formatView: TextView
	private lateinit var format: BarcodeFormat

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View {
		activity.setTitle(R.string.content)

		val view = inflater.inflate(
			R.layout.fragment_decode,
			container,
			false
		)

		val content = arguments?.getString(CONTENT) ?: ""
		format = arguments?.getSerializable(FORMAT) as BarcodeFormat? ?: BarcodeFormat.QR_CODE

		contentView = view.findViewById(R.id.content)
		contentView.setText(content)
		formatView = view.findViewById(R.id.format)
		formatView.text = resources.getQuantityString(
			R.plurals.barcode_info,
			content.length,
			format.toString(),
			content.length
		)

		view.findViewById<View>(R.id.share).setOnClickListener {
			share(getContent())
		}

		return view
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_decode, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.copy_to_clipboard -> {
				copyToClipboard(getContent())
				true
			}
			R.id.open_url -> {
				openUrl(getContent())
				true
			}
			R.id.create -> {
				addFragment(
					fragmentManager,
					EncodeFragment.newInstance(getContent(), format)
				)
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun getContent(): String {
		return contentView.text.toString()
	}

	private fun copyToClipboard(text: String) {
		activity ?: return

		val cm = activity.getSystemService(
			Context.CLIPBOARD_SERVICE
		) as ClipboardManager
		cm.text = text
		Toast.makeText(
			activity,
			R.string.put_into_clipboard,
			Toast.LENGTH_SHORT
		).show()
	}

	private fun openUrl(url: String) {
		if (activity == null || url.isEmpty()) {
			return
		}
		var uri = Uri.parse(url)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			uri = uri.normalizeScheme()
		}
		val intent = Intent(Intent.ACTION_VIEW, uri)
		if (intent.resolveActivity(activity.packageManager) != null) {
			startActivity(intent)
		} else {
			Toast.makeText(
				activity,
				R.string.cannot_resolve_action,
				Toast.LENGTH_SHORT
			).show()
		}
	}

	private fun share(text: String) {
		val intent = Intent(Intent.ACTION_SEND)
		intent.putExtra(Intent.EXTRA_TEXT, text)
		intent.type = "text/plain"
		startActivity(intent)
	}

	companion object {
		private const val CONTENT = "content"
		private const val FORMAT = "format"

		fun newInstance(content: String, format: BarcodeFormat): Fragment {
			val args = Bundle()
			args.putString(CONTENT, content)
			args.putSerializable(FORMAT, format)
			val fragment = DecodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}
