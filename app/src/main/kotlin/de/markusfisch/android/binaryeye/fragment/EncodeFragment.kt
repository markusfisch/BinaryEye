package de.markusfisch.android.binaryeye.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.zxing.BarcodeFormat
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.app.setWindowInsetListener
import de.markusfisch.android.binaryeye.view.setPadding

class EncodeFragment : Fragment() {
	private lateinit var formatView: Spinner
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
		formatView.adapter = ArrayAdapter(
			ac,
			android.R.layout.simple_list_item_1,
			writers.map { it.name }
		)

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

		setWindowInsetListener { insets ->
			(view.findViewById(R.id.inset_layout) as View).setPadding(insets)
			(view.findViewById(R.id.scroll_view) as View).setPadding(insets)
		}

		return view
	}

	override fun onPause() {
		super.onPause()
		prefs.indexOfLastSelectedFormat = formatView.selectedItemPosition
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_encode, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.next -> {
				encode()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun encode() {
		val format = writers[formatView.selectedItemPosition]
		val size = getSize(sizeBarView.progress)
		val content = contentView.text.toString()
		if (content.isEmpty()) {
			Toast.makeText(
				context,
				R.string.error_no_content,
				Toast.LENGTH_SHORT
			).show()
		} else {
			hideSoftKeyboard(contentView)
			addFragment(
				fragmentManager,
				BarcodeFragment.newInstance(content, format, size)
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
