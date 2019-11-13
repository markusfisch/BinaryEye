package de.markusfisch.android.binaryeye.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.ClipboardManager
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.ActionRegistry
import de.markusfisch.android.binaryeye.app.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class DecodeFragment : Fragment() {
	private lateinit var contentView: EditText
	private lateinit var formatView: TextView
	private lateinit var hexView: TextView
	private lateinit var format: BarcodeFormat
	private lateinit var actionMenuItem: MenuItem

	private var action = ActionRegistry.DEFAULT_ACTION
	private var isBinary = false
	private val content: String
		get() = contentView.text.toString()

	private val parentJob = Job()
	private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View {
		activity?.setTitle(R.string.content)

		val view = inflater.inflate(
			R.layout.fragment_decode,
			container,
			false
		)

		val inputContent = arguments?.getString(CONTENT) ?: ""
		isBinary = hasNonPrintableCharacters(inputContent) or inputContent.isEmpty()
		val raw = arguments?.getByteArray(RAW) ?: inputContent.toByteArray()
		format = arguments?.getSerializable(FORMAT) as BarcodeFormat? ?: BarcodeFormat.QR_CODE

		contentView = view.findViewById(R.id.content)
		val shareFab = view.findViewById<ImageView>(R.id.share)

		if (!isBinary) {
			contentView.setText(inputContent)
			contentView.addTextChangedListener(object : TextWatcher {
				override fun afterTextChanged(s: Editable?) {
					updateViewsAndAction(content.toByteArray())
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
				shareText(v.context, content)
			}
		} else {
			contentView.setText(R.string.binary_data)
			contentView.isEnabled = false
			shareFab.setImageResource(R.drawable.ic_action_save)
			shareFab.setOnClickListener {
				askForFileNameAndSave(raw)
			}
		}

		formatView = view.findViewById(R.id.format)
		hexView = view.findViewById(R.id.hex)

		updateViewsAndAction(raw)

		return view
	}

	private fun updateViewsAndAction(bytes: ByteArray) {
		val prevAction = action
		if (!prevAction.canExecuteOn(bytes)) {
			action = ActionRegistry.getAction(bytes)
		}
		formatView.text = resources.getQuantityString(
			R.plurals.barcode_info,
			bytes.size,
			format.toString(),
			bytes.size
		)
		hexView.text = hexDump(bytes, 33)
		if (::actionMenuItem.isInitialized && prevAction !== action) {
			actionMenuItem.setIcon(action.iconResId)
			actionMenuItem.setTitle(action.titleResId)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_decode, menu)
		actionMenuItem = menu.findItem(R.id.open_url)
		actionMenuItem.setIcon(action.iconResId)
		actionMenuItem.setTitle(action.titleResId)
		if (isBinary) {
			menu.findItem(R.id.copy_to_clipboard).isVisible = false
			menu.findItem(R.id.open_url).isVisible = false
			menu.findItem(R.id.create).isVisible = false
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.copy_to_clipboard -> {
				copyToClipboard(content)
				true
			}
			R.id.open_url -> {
				executeAction(content.toByteArray())
				true
			}
			R.id.create -> {
				addFragment(
					fragmentManager,
					EncodeFragment.newInstance(content, format)
				)
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
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

	private fun executeAction(content: ByteArray) {
		if (activity != null && content.isNotEmpty()) scope.launch {
			action.execute(activity, content)
		}
	}

	private fun askForFileNameAndSave(raw: ByteArray) {
		val ac = activity ?: return
		if (!hasWritePermission(ac)) return
		scope.launch(Dispatchers.Main) {
			val name = ac.askForFileName() ?: return@launch
			val message = flowOf(raw).writeToFile(name)
			Toast.makeText(
				ac,
				message,
				Toast.LENGTH_SHORT
			).show()
		}
	}

	override fun onDestroyView() {
		parentJob.cancel()
		super.onDestroyView()
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
