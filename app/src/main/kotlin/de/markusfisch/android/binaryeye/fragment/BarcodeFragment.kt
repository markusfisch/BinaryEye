package de.markusfisch.android.binaryeye.fragment

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.zxing.Zxing
import de.markusfisch.android.binaryeye.R

import android.os.Bundle
import android.graphics.Bitmap
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast

class BarcodeFragment : Fragment() {
	companion object {
		val CONTENT = "content"
		val FORMAT = "format"

		fun newInstance(content: String, format: String): Fragment {
			val args = Bundle()
			args.putString(CONTENT, content)
			args.putString(FORMAT, format)
			val fragment = BarcodeFragment()
			fragment.setArguments(args)
			return fragment
		}
	}

	override fun onCreateView(
			inflater: LayoutInflater,
			container: ViewGroup?,
			state: Bundle?): View? {
		activity.setTitle(R.string.view_barcode)

		val view = inflater.inflate(
				R.layout.fragment_barcode,
				container,
				false)

		val args = getArguments()
		args?.let {
			val metrics = DisplayMetrics()
			activity.getWindowManager().getDefaultDisplay()
					.getMetrics(metrics)
			val size = Math.min(metrics.widthPixels, metrics.heightPixels)

			val bitmap: Bitmap?
			try {
				bitmap = Zxing.encodeAsBitmap(
						args.getString(CONTENT),
						getBarcodeFormat(args.getString(FORMAT)),
						size,
						size)
			} catch (e: Exception) {
				Toast.makeText(activity, e.message,
						Toast.LENGTH_SHORT).show()
				fragmentManager.popBackStack()
				return null
			}
			view.findViewById<ImageView>(R.id.barcode).setImageBitmap(bitmap)
		}

		return view
	}

	private fun getBarcodeFormat(text: String): BarcodeFormat {
		return when (text) {
			getString(R.string.aztec) -> BarcodeFormat.AZTEC
			getString(R.string.codabar) -> BarcodeFormat.CODABAR
			getString(R.string.code_39) -> BarcodeFormat.CODE_39
			getString(R.string.code_128) -> BarcodeFormat.CODE_128
			getString(R.string.data_matrix) -> BarcodeFormat.DATA_MATRIX
			getString(R.string.ean_8) -> BarcodeFormat.EAN_8
			getString(R.string.ean_13) -> BarcodeFormat.EAN_13
			getString(R.string.itf) -> BarcodeFormat.ITF
			getString(R.string.pdf_417) -> BarcodeFormat.PDF_417
			getString(R.string.qr_code) -> BarcodeFormat.QR_CODE
			getString(R.string.upc_a) -> BarcodeFormat.UPC_A
			else -> BarcodeFormat.QR_CODE
		}
	}
}
