package de.markusfisch.android.binaryeye.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.TypefaceSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.widget.TextViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.adapter.toFormatDescriptionResId
import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.hasLocationPermission
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.content.ContentBarcode
import de.markusfisch.android.binaryeye.content.EpcQrParser
import de.markusfisch.android.binaryeye.content.IdlParser
import de.markusfisch.android.binaryeye.content.SealParser
import de.markusfisch.android.binaryeye.content.copyToClipboard
import de.markusfisch.android.binaryeye.content.epcQrToRes
import de.markusfisch.android.binaryeye.content.idlToRes
import de.markusfisch.android.binaryeye.content.shareAsFile
import de.markusfisch.android.binaryeye.content.shareText
import de.markusfisch.android.binaryeye.content.toBarcode
import de.markusfisch.android.binaryeye.content.toErrorCorrectionInt
import de.markusfisch.android.binaryeye.content.wipeLastShareFile
import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.binaryeye.database.toBundle
import de.markusfisch.android.binaryeye.database.toScan
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

class DecodeActivity : AbstractBaseActivity() {
	private lateinit var contentView: EditText
	private lateinit var formatView: TextView
	private lateinit var dataView: LinearLayout
	private lateinit var metaView: TableLayout
	private lateinit var hexView: TextView
	private lateinit var formatDescriptionView: TextView
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
	private var lastParsedDataItems: List<Field> = emptyList()
	private var showingDimmed = false
	private var label: String? = null
	private var recreationSize = 0

	override fun onActivityResult(
		requestCode: Int,
		resultCode: Int,
		resultData: Intent?
	) {
		super.onActivityResult(requestCode, resultCode, resultData)
		when (requestCode) {
			OPEN_DOCUMENT -> {
				if (resultCode == Activity.RESULT_OK) {
					val uri = resultData?.data ?: return
					openPickedFile(uri)
				}
			}
		}
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setScreenContentView(R.layout.activity_decode)
		setTitle(R.string.content)

		val justScanned = intent.hasExtra(DECODED) || intent.hasExtra(DECODED_SCAN)
		closeAutomatically = prefs.closeAutomatically && justScanned

		scan = intent?.extras?.getBundle(SCAN_BUNDLE)?.toScan()
			?: throw IllegalArgumentException("DecodeActivity needs a Scan")

		isBinary = scan.text.isEmpty()
		originalBytes = scan.raw ?: scan.text.toByteArray()
		format = scan.format.name
		recreationSize = (200f * dp).roundToInt()

		contentView = findViewById(R.id.content)
		formatView = findViewById(R.id.format)
		dataView = findViewById(R.id.data)
		metaView = findViewById(R.id.meta)
		hexView = findViewById(R.id.hex)
		formatDescriptionView = findViewById(R.id.format_description)
		stampView = findViewById(R.id.stamp)
		recreationView = findViewById(R.id.recreation)
		labelView = findViewById(R.id.label)
		fab = findViewById(R.id.open)

		if (prefs.showMetaData) {
			val descResId = format.toFormatDescriptionResId()
			if (descResId != 0) {
				formatDescriptionView.setText(descResId)
			} else {
				formatDescriptionView.visibility = View.GONE
			}
			metaView.fillMetaView(scan)
		} else {
			formatDescriptionView.visibility = View.GONE
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

		findViewById<View>(R.id.inset_layout).setPaddingFromWindowInsets()
		findViewById<View>(R.id.scroll_view).setPaddingFromWindowInsets()

		initContentAndFab(justScanned)
		updateViewsAndFab(scan.text, originalBytes)
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
		dataView.fillDataView(text, bytes, text != scan.text)
		stampView.setTrackingLink(bytes, format)
		formatView.text = resources.getQuantityString(
			R.plurals.barcode_info,
			bytes.size,
			format.prettifyFormatName(),
			bytes.size
		)
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
					scan.errorCorrectionLevel.toErrorCorrectionInt(),
					addQuietZone = true
				)
			}
			try {
				v.setImageBitmap(barcode.bitmap(recreationSize))
				v.setOnClickListener {
					v.context.hideSoftKeyboard(contentView)
					v.post {
						startActivity(
							BarcodeActivity.newIntent(this, barcode)
						)
					}
				}
			} catch (_: RuntimeException) {
				clearRecreation()
			}
		}
		hexView.showIf(prefs.showHexDump) { v ->
			v.text = hexDump(bytes)
		}
	}

	private fun clearRecreation() {
		recreationView.apply {
			setImageBitmap(null)
			setOnClickListener(null)
		}
	}

	private fun LinearLayout.fillDataView(
		text: String,
		bytes: ByteArray,
		isEditing: Boolean
	) {
		val items = mutableListOf<Field>()
		when (prefs.showChecksum) {
			"CRC4" -> items.add(Field(R.string.crc4, String.format("%X", crc4(bytes))))
			"MD5" -> items.add(Field(R.string.md5, bytes.md5().toHexString().fold()))
			"SHA1" -> items.add(Field(R.string.sha1, bytes.sha1().toHexString().fold()))
			"SHA256" -> items.add(Field(R.string.sha256, bytes.sha256().toHexString().fold()))
		}
		val count = items.count()
		parseData(items, text, bytes)
		if (items.count() > count) {
			lastParsedDataItems = items
			fillDataItems(items, false)
			return
		}
		if (isEditing && lastParsedDataItems.isNotEmpty()) {
			if (!showingDimmed) {
				fillDataItems(lastParsedDataItems, true)
			}
			return
		}
		fillDataItems(items, false)
	}

	private fun parseData(
		items: MutableList<Field>,
		text: String,
		bytes: ByteArray
	) {
		val ctx = this
		IdlParser.parse(String(bytes))?.let {
			items.add(Field("IIN", it.iin))
			it.elements.forEach { (id, value) ->
				items.add(Field(ctx.idlToRes(id), value))
			}
		}
		SealParser.parse(ctx, bytes)?.forEach { vf ->
			items.add(Field(vf.name, vf.value))
		}
		EpcQrParser.parse(text)?.let {
			items.addAll(it.map { (id, value) ->
				Field(ctx.epcQrToRes(id), value)
			})
		}
		when (action) {
			is MatMsgAction -> MatMsg(text).run {
				items.add(Field(R.string.email_to, to))
				items.add(Field(R.string.email_subject, sub))
				items.add(Field(R.string.email_body, body))
			}

			is VCardAction,
			is VEventAction -> VTypeParser.parseMap(text).forEach { item ->
				items.add(Field(item.key, item.value.joinToString("\n") { it.value }))
			}

			is WebAction -> try {
				text.toUri().run {
					items.add(Field(R.string.scheme, scheme))
					items.add(Field(R.string.host, host))
					items.add(Field(R.string.query, query))
				}
			} catch (_: Exception) {
				// Ignore
			}

			is WifiAction -> WifiConnector.parseMap(text)?.let { wifiData ->
				items.add(Field(R.string.entry_type, getString(R.string.wifi_network)))
				items.add(Field(R.string.wifi_ssid, wifiData["S"]))
				items.add(Field(R.string.wifi_password, wifiData["P"]))
				items.add(Field(R.string.wifi_type, wifiData["T"]))
				items.add(Field(R.string.wifi_hidden, wifiData["H"]))
				items.add(Field(R.string.wifi_eap, wifiData["E"]))
				items.add(Field(R.string.wifi_identity, wifiData["I"]))
				items.add(Field(R.string.wifi_anonymous_identity, wifiData["A"]))
				items.add(Field(R.string.wifi_phase2, wifiData["PH2"]))
			}
		}
	}

	private fun TableLayout.fillMetaView(scan: Scan) {
		val items = mutableListOf(
			Field(R.string.error_correction_level, scan.errorCorrectionLevel),
			Field(R.string.sequence_size, scan.sequenceSize.positiveToString()),
			Field(R.string.sequence_index, scan.sequenceIndex.positiveToString()),
			Field(R.string.sequence_id, scan.sequenceId),
			Field(R.string.gtin_country, scan.country),
			Field(R.string.gtin_add_on, scan.addOn),
			Field(R.string.gtin_price, scan.price),
			Field(R.string.gtin_issue_number, scan.issueNumber),
		)
		if (!scan.version.isNullOrBlank()) {
			val versionString = if (scan.format == ZxingCpp.BarcodeFormat.QRCode) {
				getString(
					R.string.qr_version_and_modules,
					scan.version,
					(scan.version.toIntOrNull() ?: 0) * 4 + 17
				)
			} else {
				scan.version
			}
			items.add(Field(R.string.barcode_version_number, versionString))
		}
		if (scan.format == ZxingCpp.BarcodeFormat.QRCode &&
			scan.dataMask > -1
		) {
			items.add(Field(R.string.qr_data_mask, scan.dataMask.toString()))
		}
		fillTable(items)
	}

	private fun TableLayout.fillTable(items: List<Field>) {
		removeAllViews()
		visibility = if (items.isEmpty()) View.GONE else {
			val ctx = context
			val spaceBetween = (16f * dp).roundToInt()
			items.forEach { item ->
				val text = item.value
				if (!text.isNullOrBlank()) {
					val tr = TableRow(ctx)
					val keyView = TextView(ctx).apply {
						when (val key = item.key) {
							is Int -> setText(key)
							is String -> this.text = key
							else -> this.text = key.toString()
						}
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

	private fun LinearLayout.fillDataItems(
		items: List<Field>,
		dimmed: Boolean
	) {
		showingDimmed = dimmed
		removeAllViews()
		if (items.isEmpty()) {
			visibility = View.GONE
			return
		}
		val ctx = context
		val spaceBetween = (16f * dp).roundToInt()
		for (item in items) {
			val text = item.value
			if (text.isNullOrBlank()) {
				continue
			}
			val rowView = LinearLayout(ctx).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply {
					bottomMargin = spaceBetween
				}
			}
			val keyView = TextView(ctx).apply {
				TextViewCompat.setTextAppearance(
					this,
					R.style.SecondaryText
				)
				when (val key = item.key) {
					is Int -> setText(key)
					is String -> this.text = key
					else -> this.text = key.toString()
				}
			}
			val valueView = TextView(ctx).apply {
				TextViewCompat.setTextAppearance(
					this,
					android.R.style.TextAppearance_Medium
				)
				setTextColor(
					ColorStateList.valueOf(
						ctx.obtainStyledAttributes(
							intArrayOf(android.R.attr.textColorPrimary)
						).run {
							try {
								getColor(0, currentTextColor)
							} finally {
								recycle()
							}
						}
					)
				)
				this.text = text
				setOnClickListener {
					copyToClipboard(text.toString())
				}
			}
			val alpha = if (dimmed) 0.5f else 1f
			keyView.alpha = alpha
			valueView.alpha = alpha
			rowView.addView(keyView)
			rowView.addView(valueView)
			addView(rowView)
		}
		visibility = View.VISIBLE
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_decode, menu)
		if (scan.id > 0L) {
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
				shareText(textOrHex())
				maybeBackOrFinish()
				true
			}

			R.id.share_file -> {
				val (data, name) = if (isBinary) {
					Pair(originalBytes, "barcode_content.bin")
				} else {
					Pair(content, "barcode_content.txt")
				}
				shareAsFile(data, name)
				maybeBackOrFinish()
				true
			}

			R.id.create -> {
				startActivity(
					EncodeActivity.newIntent(
						this,
						if (isBinary) originalBytes else content,
						format
					)
				)
				true
			}

			R.id.remove -> {
				askToRemoveScan(scan.id)
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun askToRemoveScan(id: Long) {
		AlertDialog.Builder(this)
			.setMessage(R.string.really_remove_scan)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				db.removeScan(id)
				backOrFinish()
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
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
		(this as android.content.Context).copyToClipboard(text, isSensitive)
		// There's a clipboard popup from Android 13 on.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
			toast(R.string.copied_to_clipboard)
		}
	}

	private fun maybeBackOrFinish() {
		if (closeAutomatically) {
			backOrFinish()
		}
	}

	private fun backOrFinish() {
		finish()
	}

	private fun executeAction(str: String) {
		if (str.isEmpty() || openLocalDocument(str)) {
			return
		}
		if (action is WifiAction &&
			Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
			!hasLocationPermission { executeAction(str) }
		) {
			return
		}
		scope.launch {
			action.execute(this@DecodeActivity, str.toByteArray())
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
		// Write permission is only required before Android Q.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
			!hasWritePermission { askForFileNameAndSave(raw) }
		) {
			return
		}
		scope.launch(Dispatchers.Main) {
			val name = askForFileName() ?: return@launch
			withContext(Dispatchers.IO) {
				val message = writeExternalFile(
					name,
					"application/octet-stream"
				) {
					it.write(raw)
				}.toSaveResult()
				withContext(Dispatchers.Main) {
					toast(message)
				}
			}
		}
	}

	companion object {
		const val DECODED_SCAN = "decoded_scan"
		const val DECODED = "decoded"
		private const val SCAN_BUNDLE = "scan_bundle"
		private const val OPEN_DOCUMENT = 1
		private const val SCHEME_FILE = "file://"

		fun newIntent(context: Context, scan: Scan): Intent {
			val args = Bundle()
			args.putBundle(SCAN_BUNDLE, scan.toBundle())
			return Intent(context, DecodeActivity::class.java).apply {
				putExtra(DECODED_SCAN, true)
				putExtras(args)
			}
		}
	}
}

private data class Field(val key: Any, val value: CharSequence?)

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
	if (format == ZxingCpp.BarcodeFormat.DataMatrix.name &&
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
