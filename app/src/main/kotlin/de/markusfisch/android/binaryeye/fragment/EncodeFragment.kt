package de.markusfisch.android.binaryeye.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.text.unescape
import de.markusfisch.android.binaryeye.view.hideSoftKeyboard
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

class EncodeFragment : Fragment() {
	private lateinit var formatView: Spinner
	private lateinit var ecLabel: TextView
	private lateinit var ecSpinner: Spinner
	private lateinit var colorsLabel: TextView
	private lateinit var colorsSpinner: Spinner
	private lateinit var sizeView: TextView
	private lateinit var sizeBarView: SeekBar
	private lateinit var marginLayout: View
	private lateinit var marginView: TextView
	private lateinit var marginBarView: SeekBar
	private lateinit var contentView: EditText
	private lateinit var unescapeCheckBox: CheckBox

	private val formats = arrayListOf(
		BarcodeFormat.AZTEC,
		BarcodeFormat.CODABAR,
		BarcodeFormat.CODE_39,
		BarcodeFormat.CODE_128,
		BarcodeFormat.DATA_MATRIX,
		BarcodeFormat.EAN_8,
		BarcodeFormat.EAN_13,
		BarcodeFormat.ITF,
		BarcodeFormat.PDF_417,
		BarcodeFormat.QR_CODE,
		BarcodeFormat.UPC_A
	)

	private var minMargin = 0
	private var bytes: ByteArray? = null

	override fun onActivityResult(
		requestCode: Int,
		resultCode: Int,
		resultData: Intent?
	) {
		when (requestCode) {
			PICK_FILE_RESULT_CODE -> {
				if (resultCode == Activity.RESULT_OK &&
					resultData != null &&
					resultData.data != null
				) {
					val ac = activity ?: return
					ac.hideSoftKeyboard(contentView)
					val uri = resultData.data ?: return
					bytes = ac.contentResolver?.openInputStream(uri)?.use {
						it.readBytesMax(4096)
					} ?: return
					setEncodeByteArray()
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
	): View? {
		val ac = activity ?: return null
		ac.setTitle(R.string.compose_barcode)

		val view = inflater.inflate(
			R.layout.fragment_encode,
			container,
			false
		)

		formatView = view.findViewById(R.id.format)
		val formatAdapter = ArrayAdapter(
			ac,
			android.R.layout.simple_spinner_item,
			formats.map { prettifyFormatName(it.name) }
		)
		formatAdapter.setDropDownViewResource(
			android.R.layout.simple_spinner_dropdown_item
		)
		formatView.adapter = formatAdapter
		formatView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(
				parentView: AdapterView<*>?,
				selectedItemView: View?,
				position: Int,
				id: Long
			) {
				val format = formats[position]
				val arrayId = when (format) {
					BarcodeFormat.AZTEC -> R.array.aztec_error_correction_levels
					BarcodeFormat.QR_CODE -> R.array.qr_error_correction_levels
					BarcodeFormat.PDF_417 -> R.array.pdf417_error_correction_levels
					else -> 0
				}
				if (arrayId > 0) {
					ecSpinner.setEntries(arrayId)
					val idx = format.unpackEcLevel(
						prefs.indexOfLastSelectedEcLevel
					)
					if (idx < ecSpinner.adapter.count) {
						ecSpinner.setSelection(idx)
					}
					true
				} else {
					false
				}.setVisibilityFor(ecLabel, ecSpinner)
				format.canBeInverted().setVisibilityFor(
					colorsLabel, colorsSpinner
				)
				when (format) {
					BarcodeFormat.AZTEC -> setMarginBar(4, 4)
					BarcodeFormat.DATA_MATRIX -> setMarginBar(1, 1)
					BarcodeFormat.QR_CODE -> setMarginBar(4, 4)
					BarcodeFormat.PDF_417 -> setMarginBar(0, 30)
					else -> false
				}.setVisibilityFor(marginLayout)
			}

			override fun onNothingSelected(parentView: AdapterView<*>?) {}
		}

		ecLabel = view.findViewById(R.id.error_correction_label)
		ecSpinner = view.findViewById(R.id.error_correction_level)
		ecSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(
				parentView: AdapterView<*>?,
				selectedItemView: View?,
				position: Int,
				id: Long
			) {
				val format = formats[formatView.selectedItemPosition]
				prefs.indexOfLastSelectedEcLevel = format.packEcLevel(
					prefs.indexOfLastSelectedEcLevel,
					position
				)
			}

			override fun onNothingSelected(parentView: AdapterView<*>?) {}
		}

		colorsLabel = view.findViewById(R.id.colors_label)
		colorsSpinner = view.findViewById(R.id.colors)

		sizeView = view.findViewById(R.id.size_display)
		sizeBarView = view.findViewById(R.id.size_bar)
		initSizeBar()

		marginLayout = view.findViewById(R.id.margin)
		marginView = view.findViewById(R.id.margin_display)
		marginBarView = view.findViewById(R.id.margin_bar)
		initMarginBar()

		contentView = view.findViewById(R.id.content)
		unescapeCheckBox = view.findViewById(R.id.unescape)
		unescapeCheckBox.isChecked = prefs.expandEscapeSequences

		val args = arguments
		args?.getString(CONTENT_TEXT)?.let {
			contentView.setText(it)
		}
		args?.getByteArray(CONTENT_RAW)?.let {
			bytes = it
			setEncodeByteArray()
		}

		val barcodeFormat = args?.getString(FORMAT)
		if (barcodeFormat != null) {
			formatView.setSelection(
				formats.indexOf(barcodeFormat.toFormat())
			)
		} else if (state == null) {
			formatView.post {
				formatView.setSelection(prefs.indexOfLastSelectedFormat)
			}
		}

		view.findViewById<View>(R.id.encode).setOnClickListener {
			it.context.encode()
		}

		(view.findViewById(R.id.inset_layout) as View).setPaddingFromWindowInsets()
		(view.findViewById(R.id.scroll_view) as View).setPaddingFromWindowInsets()

		return view
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_encode, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.pick_file -> {
				startActivityForResult(
					Intent.createChooser(
						Intent(Intent.ACTION_GET_CONTENT).apply {
							type = "*/*"
						},
						getString(R.string.pick_file)
					),
					PICK_FILE_RESULT_CODE
				)
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onPause() {
		super.onPause()
		prefs.indexOfLastSelectedFormat = formatView.selectedItemPosition
		prefs.expandEscapeSequences = unescapeCheckBox.isChecked
		prefs.lastMargin = marginBarView.progress
	}

	private fun Context.encode() {
		hideSoftKeyboard(contentView)
		val content = getContent() ?: return
		val format = formats[formatView.selectedItemPosition]
		fragmentManager?.addFragment(
			BarcodeFragment.newInstance(
				content,
				format,
				getSize(sizeBarView.progress),
				when (format) {
					BarcodeFormat.AZTEC,
					BarcodeFormat.DATA_MATRIX,
					BarcodeFormat.QR_CODE,
					BarcodeFormat.PDF_417 -> marginBarView.progress

					else -> -1
				},
				format.getErrorCorrectionLevel(ecSpinner.selectedItemPosition),
				if (format.canBeInverted()) {
					colorsSpinner.selectedItemPosition
				} else 0
			)
		)
	}

	private fun Context.getContent(): Any? {
		if (bytes != null) {
			return bytes
		}
		var text = contentView.text.toString()
		if (unescapeCheckBox.isChecked) {
			try {
				text = text.unescape()
			} catch (e: IllegalArgumentException) {
				toast(e.message ?: "Invalid escape sequence")
				return null
			}
			// Return a ByteArray if there were escape sequences for
			// non-printable characters (like `\0`). This means the content
			// will be encoded in binary mode, what would be wrong for
			// "something\nlike\tthüs".
			if (text.any { it.code < 32 && it.code !in setOf(9, 10, 13) }) {
				return text.toByteArray()
			}
		}
		if (text.isEmpty()) {
			toast(R.string.error_no_content)
			return null
		}
		return text
	}

	private fun initSizeBar() {
		updateSize(sizeBarView.progress)
		sizeBarView.setOnSeekBarChangeListener(
			object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(
					seekBar: SeekBar,
					progressValue: Int,
					fromUser: Boolean
				) {
					updateSize(progressValue)
				}

				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onStopTrackingTouch(seekBar: SeekBar) {}
			})
	}

	private fun updateSize(power: Int) {
		val size = getSize(power)
		sizeView.text = getString(R.string.size_width_by_height, size, size)
	}

	private fun initMarginBar() {
		updateMargin(marginBarView.progress)
		marginBarView.setOnSeekBarChangeListener(
			object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(
					seekBar: SeekBar,
					progressValue: Int,
					fromUser: Boolean
				) {
					if (progressValue >= minMargin) {
						updateMargin(progressValue)
					}
				}

				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onStopTrackingTouch(seekBar: SeekBar) {}
			})
	}

	private fun updateMargin(margin: Int) {
		marginView.text = getString(R.string.margin_size, margin)
	}

	private fun setMarginBar(minimum: Int, value: Int): Boolean {
		minMargin = minimum
		marginBarView.apply {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				min = minimum
			}
			progress = max(
				minimum, if (prefs.lastMargin > -1) {
					prefs.lastMargin
				} else {
					value
				}
			)
		}
		return true
	}

	private fun setEncodeByteArray() {
		contentView.text = null
		contentView.hint = getString(R.string.binary_data)
		contentView.isEnabled = false
		unescapeCheckBox.isEnabled = false
	}

	companion object {
		private const val CONTENT_TEXT = "content_text"
		private const val CONTENT_RAW = "content_raw"
		private const val FORMAT = "format"
		private const val PICK_FILE_RESULT_CODE = 1

		fun <T> newInstance(
			content: T? = null,
			format: String? = null
		): Fragment {
			val args = Bundle()
			content?.let {
				when (content) {
					is String -> args.putString(CONTENT_TEXT, content)
					is ByteArray -> args.putByteArray(CONTENT_RAW, content)
					else -> throw IllegalArgumentException(
						"content must be a String or a ByteArray"
					)
				}
			}
			format?.let { args.putString(FORMAT, it) }
			val fragment = EncodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}

private fun Boolean.setVisibilityFor(vararg views: View) {
	val visibility = if (this) View.VISIBLE else View.GONE
	for (view in views) {
		view.visibility = visibility
	}
}

private fun BarcodeFormat.packEcLevel(packed: Int, level: Int): Int {
	val s = ecLevelShift()
	return (level shl s) or (packed and (15 shl s).inv())
}

private fun BarcodeFormat.unpackEcLevel(packed: Int) =
	(packed shr ecLevelShift()) and 15

private fun BarcodeFormat.ecLevelShift() = when (this) {
	BarcodeFormat.AZTEC -> 0
	BarcodeFormat.QR_CODE -> 4
	BarcodeFormat.PDF_417 -> 8
	else -> throw IllegalArgumentException("$this does not have error levels")
}

private fun BarcodeFormat.canBeInverted() = when (this) {
	BarcodeFormat.AZTEC,
	BarcodeFormat.DATA_MATRIX,
	BarcodeFormat.QR_CODE -> true

	else -> false
}

private fun String.toFormat(default: BarcodeFormat = BarcodeFormat.QR_CODE): BarcodeFormat = try {
	BarcodeFormat.valueOf(this)
} catch (_: IllegalArgumentException) {
	default
}

private fun BarcodeFormat.getErrorCorrectionLevel(position: Int) = when (this) {
	BarcodeFormat.AZTEC -> position
	BarcodeFormat.QR_CODE -> (position + 1) * 2
	BarcodeFormat.PDF_417 -> position
	else -> 0
}.coerceIn(0, 8)

private fun Spinner.setEntries(resId: Int) = ArrayAdapter.createFromResource(
	this.context,
	resId,
	android.R.layout.simple_spinner_item
).also { aa ->
	aa.setDropDownViewResource(
		android.R.layout.simple_spinner_dropdown_item
	)
	adapter = aa
}

private fun getSize(power: Int) = 128 * (power + 1)

private fun InputStream.readBytesMax(max: Int): ByteArray {
	var offset = 0
	var remaining = min(available(), max)
	val result = ByteArray(remaining)
	while (remaining > 0) {
		val read = read(result, offset, remaining)
		if (read < 0) {
			break
		}
		remaining -= read
		offset += read
	}
	return result
}
