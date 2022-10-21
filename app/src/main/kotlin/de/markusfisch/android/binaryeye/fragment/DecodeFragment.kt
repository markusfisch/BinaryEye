package de.markusfisch.android.binaryeye.fragment

import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.ActionRegistry
import de.markusfisch.android.binaryeye.actions.wifi.WifiAction
import de.markusfisch.android.binaryeye.actions.wifi.WifiConnector
import de.markusfisch.android.binaryeye.activity.MainActivity
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.app.*
import de.markusfisch.android.binaryeye.content.copyToClipboard
import de.markusfisch.android.binaryeye.content.shareText
import de.markusfisch.android.binaryeye.content.toHexString
import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.binaryeye.io.askForFileName
import de.markusfisch.android.binaryeye.io.toSaveResult
import de.markusfisch.android.binaryeye.io.writeExternalFile
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DecodeFragment : Fragment() {
	private lateinit var contentView: EditText
	private lateinit var formatView: TextView
	private lateinit var dataView: TableLayout
	private lateinit var metaView: TableLayout
	private lateinit var hexView: TextView
	private lateinit var format: String
	private lateinit var stampView: TextView
	private lateinit var fab: FloatingActionButton

	private val parentJob = Job()
	private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + parentJob)
	private val content: String
		get() = contentView.text.toString()

	private var closeAutomatically = false
	private var action = ActionRegistry.DEFAULT_ACTION
	private var isBinary = false
	private var raw: ByteArray = ByteArray(0)
	private var id = 0L

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

		closeAutomatically = prefs.closeAutomatically &&
				activity?.intent?.hasExtra(MainActivity.DECODED) == true

		val scan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			arguments?.getParcelable(SCAN, Scan::class.java)
		} else {
			@Suppress("DEPRECATION")
			arguments?.getParcelable(SCAN) as Scan?
		} ?: throw IllegalArgumentException("DecodeFragment needs a Scan")
		id = scan.id

		val inputContent = scan.content
		isBinary = scan.raw != null
		raw = scan.raw ?: inputContent.toByteArray()
		format = scan.format

		contentView = view.findViewById(R.id.content)
		stampView = view.findViewById(R.id.stamp)
		fab = view.findViewById(R.id.open)

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
			fab.setOnClickListener {
				executeAction(content.toByteArray())
			}
			if (prefs.openImmediately) {
				executeAction(content.toByteArray())
			}
		} else {
			contentView.setText(String(raw).foldNonAlNum())
			contentView.isEnabled = false
			fab.setImageResource(R.drawable.ic_action_save)
			fab.setOnClickListener {
				askForFileNameAndSave(raw)
			}
		}

		val trackingLink = generateDpTrackingLink(raw, scan.format)
		if (trackingLink != null) {
			stampView.apply {
				text = Html.fromHtml(trackingLink)
				isClickable = true
				movementMethod = LinkMovementMethod.getInstance()
			}
		} else {
			stampView.visibility = View.GONE
		}

		formatView = view.findViewById(R.id.format)
		dataView = view.findViewById(R.id.data)
		metaView = view.findViewById(R.id.meta)
		hexView = view.findViewById(R.id.hex)

		updateViewsAndAction(raw)

		if (!isBinary) {
			fillDataView(dataView, inputContent)
		}

		if (prefs.showMetaData) {
			fillMetaView(metaView, scan)
		}

		(view.findViewById(R.id.inset_layout) as View).setPaddingFromWindowInsets()
		(view.findViewById(R.id.scroll_view) as View).setPaddingFromWindowInsets()

		return view
	}

	override fun onDestroy() {
		super.onDestroy()
		parentJob.cancel()
	}

	private fun updateViewsAndAction(bytes: ByteArray) {
		val prevAction = action
		if (!prevAction.canExecuteOn(bytes)) {
			action = ActionRegistry.getAction(bytes)
		}
		formatView.text = resources.getQuantityString(
			R.plurals.barcode_info,
			bytes.size,
			prettifyFormatName(format),
			bytes.size
		)
		hexView.text = if (prefs.showHexDump) hexDump(bytes) else ""
		if (prevAction !== action) {
			fab.setImageResource(action.iconResId)
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				fab.setOnLongClickListener { v ->
					v.context.toast(action.titleResId)
					true
				}
			} else {
				fab.tooltipText = getString(action.titleResId)
			}
		}
	}

	private fun fillDataView(tableLayout: TableLayout, content: String) {
		val items = LinkedHashMap<Int, String?>()
		if (action is WifiAction) {
			WifiConnector.parseMap(content)?.let { wifiData ->
				items.putAll(
					linkedMapOf(
						R.string.entry_type to getString(R.string.wifi_network),
						R.string.wifi_ssid to wifiData["S"],
						R.string.wifi_password to wifiData["P"],
						R.string.wifi_type to wifiData["T"],
						R.string.wifi_hidden to wifiData["H"],
						R.string.wifi_eap to wifiData["E"],
						R.string.wifi_identity to wifiData["I"],
						R.string.wifi_anonymous_identity to wifiData["A"],
						R.string.wifi_phase2 to wifiData["PH2"]
					)
				)
			}
		}
		fillDataTable(tableLayout, items)
	}

	private fun fillMetaView(tableLayout: TableLayout, scan: Scan) {
		val items = linkedMapOf(
			R.string.error_correction_level to scan.errorCorrectionLevel,
			R.string.sequence_size to scan.sequenceSize.positiveToString(),
			R.string.sequence_index to scan.sequenceIndex.positiveToString(),
			R.string.sequence_id to scan.sequenceId,
			R.string.gtin_country to scan.country,
			R.string.gtin_add_on to scan.addOn,
			R.string.gtin_price to scan.price,
			R.string.gtin_issue_number to scan.issueNumber,
		)
		if (scan.versionNumber > 0) {
			val versionString = if (scan.format == "QR_CODE") {
				getString(
					R.string.qr_version_and_modules,
					scan.versionNumber,
					scan.versionNumber * 4 + 17
				)
			} else {
				scan.versionNumber.toString()
			}
			items.putAll(
				linkedMapOf(
					R.string.barcode_version_number to versionString
				)
			)
		}
		fillDataTable(tableLayout, items)
	}

	private fun fillDataTable(
		tableLayout: TableLayout,
		items: LinkedHashMap<Int, String?>
	) {
		if (items.isEmpty()) {
			tableLayout.visibility = View.GONE
			return
		}
		val ctx = tableLayout.context
		val spaceBetween = (16f * ctx.resources.displayMetrics.density).toInt()
		items.forEach { item ->
			val text = item.value
			if (!text.isNullOrBlank()) {
				val tr = TableRow(ctx)
				val keyView = TextView(ctx)
				keyView.setText(item.key)
				val valueView = TextView(ctx)
				valueView.setPadding(spaceBetween, 0, 0, 0)
				valueView.text = text
				tr.addView(keyView)
				tr.addView(valueView)
				tableLayout.addView(tr)
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_decode, menu)
		if (isBinary) {
			menu.findItem(R.id.copy_to_clipboard).isVisible = false
			menu.findItem(R.id.create).isVisible = false
		}
		if (id > 0L) {
			menu.findItem(R.id.remove).isVisible = true
		}
		if (action is WifiAction) {
			menu.findItem(R.id.copy_password).isVisible = true
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.copy_password -> {
				copyPasswordToClipboard()
				maybeBackOrFinish()
				true
			}
			R.id.copy_to_clipboard -> {
				copyToClipboard(textOrHex())
				maybeBackOrFinish()
				true
			}
			R.id.share -> {
				context?.apply {
					shareText(textOrHex())
					maybeBackOrFinish()
				}
				true
			}
			R.id.create -> {
				fragmentManager?.addFragment(
					EncodeFragment.newInstance(content, format)
				)
				true
			}
			R.id.remove -> {
				db.removeScan(id)
				backOrFinish()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun textOrHex() = if (isBinary) {
		raw.toHexString()
	} else {
		content
	}

	private fun copyPasswordToClipboard() {
		val ac = action
		if (ac is WifiAction) {
			ac.password?.let { password ->
				activity?.apply {
					copyToClipboard(password, isSensitive = true)
					toast(R.string.copied_password_to_clipboard)
				}
			}
		}
	}

	private fun copyToClipboard(text: String, isSensitive: Boolean = false) {
		activity?.apply {
			copyToClipboard(text, isSensitive)
			toast(R.string.copied_to_clipboard)
		}
	}

	private fun maybeBackOrFinish() {
		if (closeAutomatically) {
			backOrFinish()
		}
	}

	private fun backOrFinish() {
		val fm = fragmentManager
		if (fm != null && fm.backStackEntryCount > 0) {
			fm.popBackStack()
		} else {
			activity?.finish()
		}
	}

	private fun executeAction(content: ByteArray) {
		val ac = activity ?: return
		if (content.isNotEmpty()) {
			if (action is WifiAction &&
				Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
				!ac.hasLocationPermission { executeAction(content) }
			) {
				return
			}
			scope.launch {
				action.execute(ac, content)
				maybeBackOrFinish()
			}
		}
	}

	private fun askForFileNameAndSave(raw: ByteArray) {
		val ac = activity ?: return
		// Write permission is only required before Android Q.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
			!ac.hasWritePermission { askForFileNameAndSave(raw) }
		) {
			return
		}
		scope.launch(Dispatchers.Main) {
			val name = ac.askForFileName() ?: return@launch
			val message = ac.writeExternalFile(
				name,
				"application/octet-stream"
			) {
				it.write(raw)
			}.toSaveResult()
			ac.toast(message)
		}
	}

	companion object {
		private const val SCAN = "scan"

		fun newInstance(scan: Scan): Fragment {
			val args = Bundle()
			args.putParcelable(SCAN, scan)
			val fragment = DecodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}

private fun Int.positiveToString() = if (this > -1) this.toString() else ""

private val nonAlNum = "[^a-zA-Z0-9]".toRegex()
private val multipleDots = "[…]+".toRegex()
private fun String.foldNonAlNum() = replace(nonAlNum, "…")
	.replace(multipleDots, "…")

private fun hexDump(bytes: ByteArray, charsPerLine: Int = 33): String {
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
		ascii.append(if (ord > 31) ord.toInt().toChar() else " ")
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

private fun generateDpTrackingLink(raw: ByteArray, format: String): String? {
	// Check for Deutsche Post Matrixcode stamp.
	var isStamp = false
	var rawData = raw
	if (format == "DATA_MATRIX" &&
		raw.toString(Charsets.ISO_8859_1).startsWith("DEA5")
	) {
		if (raw.size == 47) {
			isStamp = true
		} else if (raw.size > 47) {
			// Transform back to original data.
			rawData = raw.toString(Charsets.UTF_8).toByteArray(
				Charsets.ISO_8859_1
			)
			if (rawData.size == 47) {
				isStamp = true
			}
		}
	}

	if (!isStamp) {
		return null
	}

	val hex = StringBuilder()
	hex.append(String.format("%02X", rawData[9]))
	hex.append(String.format("%02X", rawData[10]))
	hex.append(String.format("%02X", rawData[11]))
	hex.append(String.format("%02X", rawData[12]))
	hex.append(String.format("%02X", rawData[13]))
	hex.append(String.format("%X", (rawData[4].toInt() and 0x0f).toByte()))
	hex.append(String.format("%02X", rawData[5]))
	hex.append(String.format("%02X", rawData[6]))
	hex.append(String.format("%02X", rawData[7]))
	hex.append(String.format("%02X", rawData[8]))
	val hexString = hex.toString()
	val trackingNumber = hexString + String.format(
		"%X",
		crc4(hexString.toByteArray(Charsets.ISO_8859_1))
	)
	return "<a href=\"https://www.deutschepost.de/de/s/sendungsverfolgung/verfolgen.html?piececode=$trackingNumber\">Deutsche Post: $trackingNumber</a>"
}

// CRC-4 with polynomial x^4 + x + 1.
private fun crc4(input: ByteArray): Int {
	var crc = 0
	var i = 0
	while (i < input.size) {
		val c = input[i].toInt()
		var j = 0x80
		while (j != 0) {
			var bit = crc and 0x8
			crc = crc shl 1
			if (c and j != 0) {
				bit = bit xor 0x8
			}
			if (bit != 0) {
				crc = crc xor 0x3
			}
			j = j ushr 1
		}
		++i
	}
	crc = crc and 0xF
	return crc
}
