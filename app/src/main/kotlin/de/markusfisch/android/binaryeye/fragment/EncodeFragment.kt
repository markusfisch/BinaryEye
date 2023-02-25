package de.markusfisch.android.binaryeye.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.view.hideSoftKeyboard
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.zxingcpp.ZxingCpp.Format

class EncodeFragment : Fragment() {
	private lateinit var formatView: Spinner
	private lateinit var ecLabel: TextView
	private lateinit var ecSpinner: Spinner
	private lateinit var colorsLabel: TextView
	private lateinit var colorsSpinner: Spinner
	private lateinit var sizeView: TextView
	private lateinit var sizeBarView: SeekBar
	private lateinit var contentView: EditText

	private val formats = arrayListOf(
		Format.AZTEC,
		Format.CODABAR,
		Format.CODE_39,
		Format.CODE_128,
		Format.DATA_MATRIX,
		Format.EAN_8,
		Format.EAN_13,
		Format.ITF,
		Format.PDF_417,
		Format.QR_CODE,
		Format.UPC_A
	)

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
					Format.AZTEC -> R.array.aztec_error_correction_levels
					Format.QR_CODE -> R.array.qr_error_correction_levels
					Format.PDF_417 -> R.array.pdf417_error_correction_levels
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
				}.setVisibility(ecLabel, ecSpinner)
				format.canBeInverted().setVisibility(
					colorsLabel, colorsSpinner
				)
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

		contentView = view.findViewById(R.id.content)

		val args = arguments
		args?.getString(CONTENT)?.let {
			contentView.setText(it)
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

	override fun onPause() {
		super.onPause()
		prefs.indexOfLastSelectedFormat = formatView.selectedItemPosition
	}

	private fun Context.encode() {
		val content = contentView.text.toString()
		if (content.isEmpty()) {
			toast(R.string.error_no_content)
			return
		}
		hideSoftKeyboard(contentView)
		val format = formats[formatView.selectedItemPosition]
		fragmentManager?.addFragment(
			BarcodeFragment.newInstance(
				content,
				format,
				getSize(sizeBarView.progress),
				format.getErrorCorrectionLevel(ecSpinner.selectedItemPosition),
				if (format.canBeInverted()) {
					colorsSpinner.selectedItemPosition
				} else 0
			)
		)
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
		sizeView.text = getString(R.string.width_by_height, size, size)
	}

	companion object {
		private const val CONTENT = "content"
		private const val FORMAT = "format"

		fun newInstance(
			content: String? = null,
			format: String? = null
		): Fragment {
			val args = Bundle()
			content?.let { args.putString(CONTENT, content) }
			format?.let { args.putString(FORMAT, it) }
			val fragment = EncodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}

private fun Boolean.setVisibility(vararg views: View) {
	val visibility = if (this) View.VISIBLE else View.GONE
	for (view in views) {
		view.visibility = visibility
	}
}

private fun Format.packEcLevel(packed: Int, level: Int): Int {
	val s = ecLevelShift()
	return (level shl s) or (packed and (15 shl s).inv())
}

private fun Format.unpackEcLevel(packed: Int) =
	(packed shr ecLevelShift()) and 15

private fun Format.ecLevelShift() = when (this) {
	Format.AZTEC -> 0
	Format.QR_CODE -> 4
	Format.PDF_417 -> 8
	else -> throw IllegalArgumentException("$this does not have error levels")
}

private fun Format.canBeInverted() = when (this) {
	Format.AZTEC,
	Format.DATA_MATRIX,
	Format.QR_CODE -> true
	else -> false
}

private fun String.toFormat(default: Format = Format.QR_CODE): Format = try {
	Format.valueOf(this)
} catch (_: IllegalArgumentException) {
	default
}

private fun Format.getErrorCorrectionLevel(position: Int) = when (this) {
	Format.AZTEC -> position
	Format.QR_CODE -> (position + 1) * 2
	Format.PDF_417 -> position
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
