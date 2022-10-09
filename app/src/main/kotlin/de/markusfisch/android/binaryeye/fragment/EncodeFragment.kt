package de.markusfisch.android.binaryeye.fragment

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
import de.markusfisch.android.zxingcpp.ZxingCpp.Format

class EncodeFragment : Fragment() {
	private lateinit var formatView: Spinner
	private lateinit var errorCorrectionLabel: TextView
	private lateinit var errorCorrectionLevel: Spinner
	private lateinit var sizeView: TextView
	private lateinit var sizeBarView: SeekBar
	private lateinit var contentView: EditText

	private val writers = arrayListOf(
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
		Format.MICRO_QR_CODE,
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
					writers[position] == Format.QR_CODE
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
		args?.getString(CONTENT)?.let {
			contentView.setText(it)
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
		val content = contentView.text.toString()
		if (content.isEmpty()) {
			Toast.makeText(
				context,
				R.string.error_no_content,
				Toast.LENGTH_SHORT
			).show()
		} else {
			activity?.hideSoftKeyboard(contentView)
			fragmentManager?.addFragment(
				BarcodeFragment.newInstance(
					content,
					writers[formatView.selectedItemPosition],
					getSize(sizeBarView.progress),
					errorCorrectionLevel.selectedItemPosition
				)
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

private fun resolveFormat(name: String): Format = try {
	Format.valueOf(name)
} catch (_: IllegalArgumentException) {
	Format.QR_CODE
}
