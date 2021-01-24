package de.markusfisch.android.binaryeye.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.adapter.prettifyFormatName
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.view.hideSoftKeyboard
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import java.util.*

class EncodeFragment : Fragment() {
	private lateinit var formatView: Spinner
	private lateinit var errorCorrectionLabel: TextView
	private lateinit var errorCorrectionLevel: Spinner
	private lateinit var sizeView: TextView
	private lateinit var sizeBarView: SeekBar
	private lateinit var contentView: EditText

	private val writers = arrayListOf(
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
			writers.map { prettifyFormatName(it.name) }
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
				val visibility = if (
					writers[position] == BarcodeFormat.QR_CODE
				) {
					View.VISIBLE
				} else {
					View.GONE
				}
				errorCorrectionLabel.visibility = visibility
				errorCorrectionLevel.visibility = visibility
			}

			override fun onNothingSelected(parentView: AdapterView<*>?) {}
		}

		errorCorrectionLabel = view.findViewById(R.id.error_correction_label)
		errorCorrectionLevel = view.findViewById(R.id.error_correction_level)

		sizeView = view.findViewById(R.id.size_display)
		sizeBarView = view.findViewById(R.id.size_bar)
		initSizeBar()

		contentView = view.findViewById(R.id.content)

		val args = arguments
		args?.also {
			contentView.setText(it.getString(CONTENT))
		}

		val barcodeFormat = args?.getString(FORMAT)
		if (barcodeFormat != null) {
			formatView.setSelection(
				writers.indexOf(resolveFormat(barcodeFormat))
			)
		} else if (state == null) {
			formatView.post {
				formatView.setSelection(prefs.indexOfLastSelectedFormat)
			}
		}

		view.findViewById<View>(R.id.encode).setOnClickListener {
			encode()
		}

		(view.findViewById(R.id.inset_layout) as View).setPaddingFromWindowInsets()
		(view.findViewById(R.id.scroll_view) as View).setPaddingFromWindowInsets()

		return view
	}

	override fun onPause() {
		super.onPause()
		prefs.indexOfLastSelectedFormat = formatView.selectedItemPosition
	}

	private fun encode() {
		val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
		val format = writers[formatView.selectedItemPosition]
		if (format == BarcodeFormat.QR_CODE) {
			hints[EncodeHintType.ERROR_CORRECTION] = arrayListOf(
				ErrorCorrectionLevel.L,
				ErrorCorrectionLevel.M,
				ErrorCorrectionLevel.Q,
				ErrorCorrectionLevel.H
			)[errorCorrectionLevel.selectedItemPosition]
		}
		val size = getSize(sizeBarView.progress)
		val content = contentView.text.toString()
		if (content.isEmpty()) {
			Toast.makeText(
				context,
				R.string.error_no_content,
				Toast.LENGTH_SHORT
			).show()
		} else {
			activity?.hideSoftKeyboard(contentView)
			addFragment(
				fragmentManager,
				BarcodeFragment.newInstance(content, format, size, hints)
			)
		}
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

	private fun getSize(power: Int) = 128 * (power + 1)

	companion object {
		private const val CONTENT = "content"
		private const val FORMAT = "format"

		fun newInstance(
			content: String,
			format: String? = null
		): Fragment {
			val args = Bundle()
			args.putString(CONTENT, content)
			format?.let { args.putString(FORMAT, it) }
			val fragment = EncodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}

private fun resolveFormat(name: String): BarcodeFormat = try {
	BarcodeFormat.valueOf(name)
} catch (_: IllegalArgumentException) {
	BarcodeFormat.QR_CODE
}
