package de.markusfisch.android.binaryeye.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.ActionRegistry
import de.markusfisch.android.binaryeye.actions.mail.MatMsg
import de.markusfisch.android.binaryeye.actions.mail.MatMsgAction
import de.markusfisch.android.binaryeye.actions.vtype.VTypeParser
import de.markusfisch.android.binaryeye.actions.vtype.vcard.VCardAction
import de.markusfisch.android.binaryeye.actions.vtype.vevent.VEventAction
import de.markusfisch.android.binaryeye.actions.web.WebAction
import de.markusfisch.android.binaryeye.actions.wifi.WifiAction
import de.markusfisch.android.binaryeye.actions.wifi.WifiConnector
import de.markusfisch.android.binaryeye.activity.MainActivity
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.hasLocationPermission
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.content.ContentBarcode
import de.markusfisch.android.binaryeye.content.IdlParser
import de.markusfisch.android.binaryeye.content.copyToClipboard
import de.markusfisch.android.binaryeye.content.idlToRes
import de.markusfisch.android.binaryeye.content.shareAsFile
import de.markusfisch.android.binaryeye.content.shareText
import de.markusfisch.android.binaryeye.content.toBarcode
import de.markusfisch.android.binaryeye.content.toErrorCorrectionInt
import de.markusfisch.android.binaryeye.content.wipeLastShareFile
import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.binaryeye.io.askForFileName
import de.markusfisch.android.binaryeye.io.toSaveResult
import de.markusfisch.android.binaryeye.io.writeExternalFile
import de.markusfisch.android.binaryeye.net.createEncodeDeeplink
import de.markusfisch.android.binaryeye.view.hideSoftKeyboard
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.zxingcpp.ZxingCpp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.math.roundToInt

class DecodeFragment : Fragment() {
	private lateinit var contentView: EditText
	private lateinit var formatView: TextView
	private lateinit var dataView: TableLayout
	private lateinit var metaView: TableLayout
	private lateinit var hexView: TextView
	private lateinit var stampView: TextView
	private lateinit var recreationView: ImageView
	private lateinit var labelView: EditText
	private lateinit var fab: FloatingActionButton
	private lateinit var scan: Scan
	private lateinit var format: String

	private val parentJob = Job()
	private val scope: CoroutineScope = CoroutineScope(
		Dispatchers.Main + parentJob
	)
	private val dp: Float
		get() = resources.displayMetrics.density
	private val content: String
		get() = contentView.text.toString()

	private var closeAutomatically = false
	private var action = ActionRegistry.DEFAULT_ACTION
	private var isBinary = false
	private var originalBytes: ByteArray = ByteArray(0)
	private var label: String? = null
	private var recreationSize = 0

	override fun onActivityResult(
		requestCode: Int,
		resultCode: Int,
		resultData: Intent?
	) {
		when (requestCode) {
			OPEN_DOCUMENT -> {
				if (resultCode == Activity.RESULT_OK) {
					val uri = resultData?.data ?: return
					activity?.openPickedFile(uri)
				}
			}
		}
	}

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

		val justScanned = activity?.intent?.hasExtra(
			MainActivity.DECODED
		) == true
		closeAutomatically = prefs.closeAutomatically && justScanned

		scan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			arguments?.getParcelable(SCAN, Scan::class.java)
		} else {
			@Suppress("DEPRECATION")
			arguments?.getParcelable(SCAN)
		} ?: throw IllegalArgumentException("DecodeFragment needs a Scan")

		isBinary = scan.text.isEmpty()
		originalBytes = scan.raw ?: scan.text.toByteArray()
		format = scan.format.name
		recreationSize = (200f * dp).roundToInt()

		contentView = view.findViewById(R.id.content)
		formatView = view.findViewById(R.id.format)
		dataView = view.findViewById(R.id.data)
		metaView = view.findViewById(R.id.meta)
		hexView = view.findViewById(R.id.hex)
		stampView = view.findViewById(R.id.stamp)
		recreationView = view.findViewById(R.id.recreation)
		labelView = view.findViewById(R.id.label)
		fab = view.findViewById(R.id.open)

		if (prefs.showMetaData) {
			metaView.fillMetaView(scan)
		} else {
			metaView.visibility = View.GONE
		}

		if (scan.id > 0) {
			scan.label?.let {
				labelView.setText(it)
				label = it
			}
		} else {
			labelView.visibility = View.GONE
		}

		view.findViewById<View>(R.id.inset_layout).setPaddingFromWindowInsets()
		view.findViewById<View>(R.id.scroll_view).setPaddingFromWindowInsets()

		initContentAndFab(justScanned)
		updateViewsAndFab(scan.text, originalBytes)

		return view
	}

	override fun onPause() {
		super.onPause()
		val id = scan.id
		if (id > 0) {
			val newLabel = labelView.text.toString().trim()
			if (newLabel != (label ?: "")) {
				db.renameScan(id, newLabel)
				label = newLabel
			}
		}
		if (action.fired) {
			maybeBackOrFinish()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		parentJob.cancel()
		wipeLastShareFile()
	}

	private fun initContentAndFab(justScanned: Boolean) {
		if (isBinary) {
			contentView.setText(String(originalBytes).foldNonAlNum())
			contentView.isEnabled = false
			fab.setImageResource(R.drawable.ic_action_save)
			fab.setOnClickListener {
				askForFileNameAndSave(originalBytes)
			}
		} else {
			contentView.setText(scan.text)
			contentView.addTextChangedListener(object : TextWatcher {
				override fun afterTextChanged(s: Editable?) {
					updateViewsAndFab(s?.toString() ?: "")
				}

				override fun beforeTextChanged(
					s: CharSequence?,
					start: Int,
					count: Int,
					after: Int
				) = Unit

				override fun onTextChanged(
					s: CharSequence?,
					start: Int,
					before: Int,
					count: Int
				) = Unit
			})
			fab.setOnClickListener {
				executeAction(content)
			}
			if (justScanned && prefs.openImmediately) {
				closeAutomatically = true
				executeAction(content)
			}
		}
	}

	private fun updateViewsAndFab(text: String, bytes: ByteArray? = null) {
		val b = bytes ?: text.toByteArray()
		resolveActionAndUpdateFab(b)
		updateViews(text, b)
	}

	private fun resolveActionAndUpdateFab(bytes: ByteArray) {
		if (!isBinary) {
			val prevAction = action
			if (!prevAction.canExecuteOn(bytes)) {
				action = ActionRegistry.getAction(bytes)
			}
			if (prevAction !== action) {
				fab.setImageResource(action.iconResId)
			}
		}
		// Run this for the initial action too.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			fab.setOnLongClickListener { v ->
				v.context.toast(action.titleResId)
				true
			}
		} else {
			fab.tooltipText = getString(action.titleResId)
		}
	}

	private fun updateViews(text: String, bytes: ByteArray) {
		formatView.text = resources.getQuantityString(
			R.plurals.barcode_info,
			bytes.size,
			format.prettifyFormatName(),
			bytes.size
		)
		hexView.showIf(prefs.showHexDump) { v ->
			v.text = hexDump(bytes)
		}
		recreationView.showIf(prefs.showRecreation) { v ->
			val unmodified: Boolean
			val content = if (isBinary) {
				if (bytes.isEmpty()) {
					clearRecreation()
					return@showIf
				}
				unmodified = bytes.contentEquals(originalBytes)
				bytes
			} else {
				if (text.isEmpty()) {
					clearRecreation()
					return@showIf
				}
				unmodified = text == scan.text
				text
			}
			val barcode = if (unmodified) {
				scan.toBarcode()
			} else {
				ContentBarcode(
					content,
					scan.format,
					scan.errorCorrectionLevel.toErrorCorrectionInt()
				)
			}
			try {
				v.setImageBitmap(barcode.bitmap(recreationSize))
				v.setOnClickListener {
					v.context.hideSoftKeyboard(contentView)
					v.post {
						fragmentManager?.addFragment(
							BarcodeFragment.newInstance(barcode)
						)
					}
				}
			} catch (_: RuntimeException) {
				clearRecreation()
			}
		}
		dataView.fillDataView(text, bytes)
		stampView.setTrackingLink(bytes, format)
	}

	private fun clearRecreation() {
		recreationView.apply {
			setImageBitmap(null)
			setOnClickListener(null)
		}
	}

	private fun TableLayout.fillDataView(text: String, bytes: ByteArray) {
		val items = LinkedHashMap<Any, CharSequence?>()
		when (prefs.showChecksum) {
			"CRC4" -> items[R.string.crc4] = String.format("%X", crc4(bytes))
			"MD5" -> items[R.string.md5] = bytes.md5().toHexString().fold()
			"SHA1" -> items[R.string.sha1] = bytes.sha1().toHexString().fold()
			"SHA256" -> items[R.string.sha256] =
				bytes.sha256().toHexString().fold()

			else -> Unit
		}
		IdlParser.parse(String(bytes))?.let {
			items.putAll(mapOf("IIN" to it.iin))
			items.putAll(
				it.elements.entries.associate { (id, value) ->
					context.idlToRes(id) to value
				}
			)
		}
		when (action) {
			is MatMsgAction -> items.putAll(
				MatMsg(text).run {
					mapOf(
						R.string.email_to to to,
						R.string.email_subject to sub,
						R.string.email_body to body
					)
				}
			)

			is VCardAction,
			is VEventAction -> VTypeParser.parseMap(text).let { vData ->
				items.putAll(
					vData.map { item ->
						item.key to item.value.joinToString("\n") {
							it.value
						}
					}.toMap()
				)
			}

			is WebAction -> try {
				items.putAll(
					Uri.parse(text).run {
						mapOf(
							R.string.scheme to scheme,
							R.string.host to host,
							R.string.query to query
						)
					}
				)
			} catch (_: Exception) {
				// Ignore
			}

			is WifiAction -> WifiConnector.parseMap(text)?.let { wifiData ->
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
		fill(items)
	}

	private fun TableLayout.fillMetaView(scan: Scan) {
		val items = linkedMapOf<Any, CharSequence?>(
			R.string.error_correction_level to scan.errorCorrectionLevel,
			R.string.sequence_size to scan.sequenceSize.positiveToString(),
			R.string.sequence_index to scan.sequenceIndex.positiveToString(),
			R.string.sequence_id to scan.sequenceId,
			R.string.gtin_country to scan.country,
			R.string.gtin_add_on to scan.addOn,
			R.string.gtin_price to scan.price,
			R.string.gtin_issue_number to scan.issueNumber,
		)
		if (!scan.version.isNullOrBlank()) {
			val versionString = if (scan.format.name == "QR_CODE") {
				getString(
					R.string.qr_version_and_modules,
					scan.version,
					(scan.version.toIntOrNull() ?: 0) * 4 + 17
				)
			} else {
				scan.version
			}
			items.putAll(
				linkedMapOf(
					R.string.barcode_version_number to versionString
				)
			)
		}
		if (scan.format == ZxingCpp.BarcodeFormat.QR_CODE &&
			scan.dataMask > -1
		) {
			items.putAll(
				linkedMapOf(
					R.string.qr_data_mask to scan.dataMask.toString()
				)
			)
		}
		fill(items)
	}

	private fun TableLayout.fill(
		items: LinkedHashMap<Any, CharSequence?>
	) {
		removeAllViews()
		visibility = if (items.isEmpty()) View.GONE else {
			val ctx = context
			val spaceBetween = (16f * dp).roundToInt()
			items.forEach { item ->
				val text = item.value
				if (!text.isNullOrBlank()) {
					val tr = TableRow(ctx)
					val keyView = TextView(ctx)
					when (val key = item.key) {
						is Int -> keyView.setText(key)
						is String -> keyView.text = key
						else -> keyView.text = key.toString()
					}
					val valueView = TextView(ctx).apply {
						setPadding(spaceBetween, 0, 0, 0)
						this.text = text
						setOnClickListener {
							copyToClipboard(text.toString())
						}
					}
					tr.addView(keyView)
					tr.addView(valueView)
					addView(tr)
				}
			}
			View.VISIBLE
		}
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_decode, menu)
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

			R.id.copy_as_deeplink -> {
				copyToClipboard(deeplinkToCopy())
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

			R.id.share_file -> {
				context?.apply {
					val (data, name) = if (isBinary) {
						Pair(originalBytes, "barcode_content.bin")
					} else {
						Pair(content, "barcode_content.txt")
					}
					shareAsFile(data, name)
					maybeBackOrFinish()
				}
				true
			}

			R.id.create -> {
				fragmentManager?.addFragment(
					EncodeFragment.newInstance(
						if (isBinary) originalBytes else content,
						format
					)
				)
				true
			}

			R.id.remove -> {
				db.removeScan(scan.id)
				backOrFinish()
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun textOrHex() = if (isBinary) {
		originalBytes.toHexString()
	} else {
		content
	}

	private fun deeplinkToCopy() = createEncodeDeeplink(
		format = format,
		content = textOrHex(),
	)

	private fun copyPasswordToClipboard() {
		val ac = action
		if (ac is WifiAction) {
			ac.password?.let { password ->
				copyToClipboard(password, isSensitive = true)
			}
		}
	}

	private fun copyToClipboard(text: String, isSensitive: Boolean = false) {
		activity?.apply {
			copyToClipboard(text, isSensitive)
			// There's a clipboard popup from Android 13 on.
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
				toast(R.string.copied_to_clipboard)
			}
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

	private fun executeAction(str: String) {
		val ac = activity ?: return
		if (str.isEmpty() || openLocalDocument(str)) {
			return
		}
		if (action is WifiAction &&
			Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
			!ac.hasLocationPermission { executeAction(str) }
		) {
			return
		}
		scope.launch {
			action.execute(ac, str.toByteArray())
		}
	}

	private fun openLocalDocument(str: String): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
			!str.startsWith(SCHEME_FILE)
		) {
			return false
		}
		// Needs to be disabled so we get the result.
		closeAutomatically = false
		// file:// can't be used with Scoped Storage anymore.
		startActivityForResult(
			Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
				addCategory(Intent.CATEGORY_OPENABLE)
				type = "*/*"
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					val primary = str.substring(
						SCHEME_FILE.length
					).localDirName() ?: Environment.getExternalStorageDirectory().path
					putExtra(
						DocumentsContract.EXTRA_INITIAL_URI,
						DocumentsContract.buildDocumentUri(
							"com.android.externalstorage.documents",
							"primary:$primary"
						)
					)
				}
			},
			OPEN_DOCUMENT
		)
		return true
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
			withContext(Dispatchers.IO) {
				val message = ac.writeExternalFile(
					name,
					"application/octet-stream"
				) {
					it.write(raw)
				}.toSaveResult()
				withContext(Dispatchers.Main) {
					ac.toast(message)
				}
			}
		}
	}

	companion object {
		private const val SCAN = "scan"
		private const val OPEN_DOCUMENT = 1
		private const val SCHEME_FILE = "file://"

		fun newInstance(scan: Scan): Fragment {
			val args = Bundle()
			args.putParcelable(SCAN, scan)
			val fragment = DecodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}

private inline fun <T : View> T.showIf(
	visible: Boolean,
	block: (T) -> Unit
) {
	visibility = if (visible) {
		block.invoke(this)
		View.VISIBLE
	} else {
		View.GONE
	}
}

private val nonAlNum = "[^a-zA-Z0-9]".toRegex()
private val multipleDots = "…+".toRegex()
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

private fun Int.positiveToString() = if (this > -1) this.toString() else ""

private fun TextView.setTrackingLink(bytes: ByteArray, format: String) {
	val trackingLink = generateDpTrackingLink(bytes, format)
	if (trackingLink != null) {
		text = trackingLink.fromHtml()
		isClickable = true
		movementMethod = LinkMovementMethod.getInstance()
	} else {
		visibility = View.GONE
	}
}

private fun generateDpTrackingLink(bytes: ByteArray, format: String): String? {
	// Check for Deutsche Post Matrixcode stamp.
	var isStamp = false
	var rawData = bytes
	if (format == "DATA_MATRIX" &&
		bytes.toString(Charsets.ISO_8859_1).startsWith("DEA5")
	) {
		if (bytes.size == 47) {
			isStamp = true
		} else if (bytes.size > 47) {
			// Transform back to original data.
			rawData = bytes.toString(Charsets.UTF_8).toByteArray(
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
	return "<a href=\"https://www.deutschepost.de/de/s/sendungsverfolgung.html?piececode=$trackingNumber\">Deutsche Post: $trackingNumber</a>"
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

private fun String.fromHtml() = if (
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
) {
	Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
} else {
	@Suppress("DEPRECATION")
	Html.fromHtml(this)
}

private fun ByteArray.md5(): ByteArray = MessageDigest.getInstance(
	"MD5"
).run {
	update(this@md5)
	digest()
}

private fun ByteArray.sha1(): ByteArray = MessageDigest.getInstance(
	"SHA-1"
).run {
	update(this@sha1)
	digest()
}

private fun ByteArray.sha256(): ByteArray = MessageDigest.getInstance(
	"SHA-256"
).run {
	update(this@sha256)
	digest()
}

private fun String.fold(): CharSequence {
	val l = length
	return if (l > 10) {
		val half = l / 2
		val s = substring(0, half) + "\n" + substring(half)
		SpannableString(s).apply {
			setSpan(
				TypefaceSpan("monospace"),
				0, s.length,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
	} else {
		this
	}
}

private fun String.localDirName(): String? = when {
	endsWith("/") -> this
	else -> File(this).parentFile?.absolutePath
}?.replaceFirst(Regex("^/storage/emulated/0/"), "")
	?.replaceFirst(Regex("^/sdcard/"), "")
	?.trim('/')

private fun Activity.openPickedFile(uri: Uri) {
	startActivity(
		Intent.createChooser(
			Intent(Intent.ACTION_VIEW).apply {
				setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
				addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			},
			getString(R.string.open_url)
		)
	)
}
