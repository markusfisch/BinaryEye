package de.markusfisch.android.binaryeye.fragment

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.zxing.Zxing
import de.markusfisch.android.binaryeye.R

import android.os.Bundle
import android.graphics.Bitmap
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast

class BarcodeFragment : Fragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		activity.setTitle(R.string.view_barcode)

		val view = inflater.inflate(
			R.layout.fragment_barcode,
			container,
			false
		)

		val args = getArguments()
		args?.let {
			val size = args.getInt(SIZE)
			val bitmap: Bitmap?
			try {
				bitmap = Zxing.encodeAsBitmap(
					args.getString(CONTENT),
					args.getSerializable(FORMAT) as BarcodeFormat,
					size,
					size
				)
			} catch (e: Exception) {
				Toast.makeText(
					activity, e.message,
					Toast.LENGTH_SHORT
				).show()
				fragmentManager.popBackStack()
				return null
			}
			view.findViewById<ImageView>(R.id.barcode).setImageBitmap(bitmap)
		}

		return view
	}

	companion object {
		private const val CONTENT = "content"
		private const val FORMAT = "format"
		private const val SIZE = "size"

		fun newInstance(
			content: String,
			format: BarcodeFormat,
			size: Int
		): Fragment {
			val args = Bundle()
			args.putString(CONTENT, content)
			args.putSerializable(FORMAT, format)
			args.putInt(SIZE, size)
			val fragment = BarcodeFragment()
			fragment.setArguments(args)
			return fragment
		}
	}
}
