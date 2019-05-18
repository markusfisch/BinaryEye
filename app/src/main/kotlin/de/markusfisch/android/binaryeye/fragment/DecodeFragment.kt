package de.markusfisch.android.binaryeye.fragment

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.shareText
import de.markusfisch.android.binaryeye.R

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.ClipboardManager
import android.text.Editable
import android.text.TextWatcher
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
	private lateinit var hexView: TextView
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
		val raw = arguments?.getByteArray(RAW)

		contentView = view.findViewById(R.id.content)
		contentView.setText(content)
		contentView.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) {
				updateFormatAndHex(contentView.text.toString().toByteArray())
			}
			override fun beforeTextChanged(
				s: CharSequence?,
				start: Int,
				count: Int,
				after: Int
			) {}
			override fun onTextChanged(
				s: CharSequence?,
				start: Int,
				before: Int,
				count: Int
			) {}
		})

		formatView = view.findViewById(R.id.format)
		hexView = view.findViewById(R.id.hex)

		view.findViewById<View>(R.id.share).setOnClickListener { v ->
			shareText(v.context, getContent())
		}

		updateFormatAndHex(if (raw != null) {
			raw
		} else {
			content.toByteArray()
		})

		return view
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
	if (charsPerLine < 4 || bytes.size < 1) {
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
