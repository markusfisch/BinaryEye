package de.markusfisch.android.binaryeye.fragment

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.R

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.inputmethod.InputMethodManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class EncodeFragment : Fragment() {
	private lateinit var formatView: Spinner
	private lateinit var sizeView: TextView
	private lateinit var sizeBarView: SeekBar
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
	): View {
		activity.setTitle(R.string.compose_barcode)

		val view = inflater.inflate(
			R.layout.fragment_encode,
			container,
			false
		)

		formatView = view.findViewById(R.id.format)
		formatView.adapter = ArrayAdapter<String>(
			activity,
			android.R.layout.simple_list_item_1,
			writers.map { it.name }
		)

		sizeView = view.findViewById(R.id.size_display)
		sizeBarView = view.findViewById(R.id.size_bar)
		initSizeBar()

		val contentView = view.findViewById<EditText>(R.id.content)

		val args = arguments
		args?.let {
			contentView.setText(args.getString(CONTENT))
			formatView.setSelection(
				writers.indexOf(
					args.getSerializable(FORMAT) as BarcodeFormat?
				)
			)
		}

		view.findViewById<View>(R.id.encode).setOnClickListener { v ->
			val format = writers[formatView.selectedItemPosition]
			val size = getSize(sizeBarView.progress)
			val content = contentView.text.toString()
			if (content.isEmpty()) {
				Toast.makeText(
					v.context,
					R.string.error_no_content,
					Toast.LENGTH_SHORT
				).show()
			} else {
				hideSoftKeyboard(contentView)
				addFragment(
					fragmentManager, BarcodeFragment.newInstance(
						content,
						format,
						size
					)
				)
			}
		}

		return view
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

	private fun hideSoftKeyboard(view: View) {
		val im = activity?.getSystemService(
			Context.INPUT_METHOD_SERVICE
		) as InputMethodManager?
		im?.let {
			im.hideSoftInputFromWindow(view.windowToken, 0)
		}
	}

	companion object {
		private const val CONTENT = "content"
		private const val FORMAT = "format"

		fun newInstance(
			content: String,
			format: BarcodeFormat = BarcodeFormat.AZTEC
		): Fragment {
			val args = Bundle()
			args.putString(CONTENT, content)
			args.putSerializable(FORMAT, format)
			val fragment = EncodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}
