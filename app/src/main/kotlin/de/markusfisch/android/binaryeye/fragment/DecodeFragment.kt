package de.markusfisch.android.binaryeye.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.text.ClipboardManager
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import com.google.zxing.BarcodeFormat
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.ActionRegistry
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.hasNonPrintableCharacters
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.shareText
import java.io.File
import java.io.IOException
import java.net.URLEncoder

class DecodeFragment : Fragment() {
	private lateinit var contentView: EditText
	private lateinit var formatView: TextView
	private lateinit var hexView: TextView
	private lateinit var format: BarcodeFormat
	private lateinit var specialActionButton: ImageButton

	private var isBinary = false

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
		isBinary = hasNonPrintableCharacters(content) or content.isEmpty()
		val raw = arguments?.getByteArray(RAW) ?: content.toByteArray()
		format = arguments?.getSerializable(FORMAT) as BarcodeFormat? ?: BarcodeFormat.QR_CODE

		contentView = view.findViewById(R.id.content) as EditText
		val shareFab = view.findViewById<View>(R.id.share)

		if (!isBinary) {
			contentView.setText(content)
			contentView.addTextChangedListener(object : TextWatcher {
				override fun afterTextChanged(s: Editable?) {
					updateFormatAndHex(getContent().toByteArray())
					maybeShowAction(getContent().toByteArray())
				}

				override fun beforeTextChanged(
					s: CharSequence?,
					start: Int,
					count: Int,
					after: Int
				) {
				}

				override fun onTextChanged(
					s: CharSequence?,
					start: Int,
					before: Int,
					count: Int
				) {
				}
			})
			shareFab.setOnClickListener { v ->
				shareText(v.context, getContent())
			}
		} else {
			contentView.setText(R.string.binary_data)
			contentView.isEnabled = false
			(shareFab as ImageView).setImageResource(R.drawable.ic_action_save)
			shareFab.setOnClickListener {
				askForFileNameAndSave(raw)
			}
		}

		formatView = view.findViewById(R.id.format)
		hexView = view.findViewById(R.id.hex)
		specialActionButton = view.findViewById(R.id.special_action)

		updateFormatAndHex(raw)
		maybeShowAction(raw)

		return view
	}

	private fun maybeShowAction(bytes: ByteArray) {
		val action = ActionRegistry.getAction(bytes)

		if (action != null) {
			specialActionButton.setImageResource(action.resourceId)
			specialActionButton.setOnClickListener { action.execute(context, bytes) }
			specialActionButton.visibility = View.VISIBLE
		} else {
			specialActionButton.visibility = View.GONE
		}
	}

	private fun updateFormatAndHex(bytes: ByteArray) {
		formatView.text = resources.getQuantityString(
			R.plurals.barcode_info,
			bytes.size,
			format.toString(),
			bytes.size
		)
		hexView.text = hexDump(bytes, 33)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_decode, menu)
		if (isBinary) {
			menu.findItem(R.id.copy_to_clipboard).isVisible = false
			menu.findItem(R.id.open_url).isVisible = false
			menu.findItem(R.id.create).isVisible = false
		}
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

	private fun openUrl(url: String, searchIfNoUrl: Boolean = true) {
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
		} else if (searchIfNoUrl) {
			pickSearchEngineAndSearch(activity, url)
		} else {
			Toast.makeText(
				activity,
				R.string.cannot_resolve_action,
				Toast.LENGTH_SHORT
			).show()
		}
	}

	private fun pickSearchEngineAndSearch(context: Context, query: String) {
		val urls = context.resources.getStringArray(
			R.array.search_engines_values
		)
		AlertDialog.Builder(context)
			.setTitle(R.string.pick_search_engine)
			.setItems(R.array.search_engines_names) { _, which ->
				openUrl(
					urls[which] + URLEncoder.encode(query, "utf-8"),
					false
				)
			}
			.show()
	}

	private fun askForFileNameAndSave(raw: ByteArray) {
		val ac = activity
		ac ?: return
		val view = ac.layoutInflater.inflate(R.layout.dialog_save_file, null)
		val editText = view.findViewById(R.id.file_name) as EditText
		AlertDialog.Builder(ac)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				if (hasWritePermission(ac)) {
					val messageId = saveByteArray(
						editText.text.toString(),
						raw
					)
					if (messageId > 0) {
						Toast.makeText(ac, messageId, Toast.LENGTH_SHORT).show()
					}
				}
			}
			.show()
	}

	companion object {
		private const val CONTENT = "content"
		private const val FORMAT = "format"
		private const val RAW = "raw"

		fun newInstance(
			content: String,
			format: BarcodeFormat,
			raw: ByteArray? = null
		): Fragment {
			val args = Bundle()
			args.putString(CONTENT, content)
			args.putSerializable(FORMAT, format)
			if (raw != null) {
				args.putByteArray(RAW, raw)
			}
			val fragment = DecodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}

private fun hexDump(bytes: ByteArray, charsPerLine: Int): String {
	if (charsPerLine < 4 || bytes.isEmpty()) {
		return ""
	}
	val dump = StringBuilder()
	val hex = StringBuilder()
	val ascii = StringBuilder()
	val itemsPerLine = (charsPerLine - 1) / 4
	val len = bytes.size
	var i = 0
	while (true) {
		val ord = bytes[i]
		hex.append(String.format("%02X ", ord))
		ascii.append(if (ord > 31) ord.toChar() else " ")

		++i

		val posInLine = i % itemsPerLine
		val atEnd = i >= len
		var atLineEnd = posInLine == 0
		if (atEnd && !atLineEnd) {
			for (j in posInLine until itemsPerLine) {
				hex.append("   ")
			}
			atLineEnd = true
		}
		if (atLineEnd) {
			dump.append(hex.toString())
			dump.append(" ")
			dump.append(ascii.toString())
			dump.append("\n")
			hex.setLength(0)
			ascii.setLength(0)
		}
		if (atEnd) {
			break
		}
	}
	return dump.toString()
}

fun saveByteArray(name: String, raw: ByteArray): Int {
	return try {
		val file = File(
			Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS
			),
			name
		)
		if (file.exists()) {
			return R.string.error_file_exists
		}
		file.writeBytes(raw)
		0
	} catch (e: IOException) {
		R.string.error_saving_binary_data
	}
}
