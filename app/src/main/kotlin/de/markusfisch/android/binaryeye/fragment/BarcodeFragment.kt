package de.markusfisch.android.binaryeye.fragment

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.app.shareFile
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

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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

		val args = arguments
		args?.let {
			val content = args.getString(CONTENT)
			val format = args.getSerializable(FORMAT) as BarcodeFormat?
			if (content != null && format != null) {
				val size = args.getInt(SIZE)
				val bitmap: Bitmap?
				try {
					bitmap = Zxing.encodeAsBitmap(
						content,
						format,
						size,
						size
					)
				} catch (e: Exception) {
					var message = e.message
					if (message == null || message.isEmpty()) {
						message = getString(R.string.error_encoding_barcode)
					}
					Toast.makeText(
						activity,
						message,
						Toast.LENGTH_SHORT
					).show()
					fragmentManager.popBackStack()
					return null
				}
				view.findViewById<ImageView>(R.id.barcode).setImageBitmap(bitmap)
				view.findViewById<View>(R.id.share).setOnClickListener {
					bitmap?.let {
						share(bitmap)
					}
				}
			}
		}

		return view
	}

	private fun share(bitmap: Bitmap) {
		GlobalScope.launch {
			val file = saveBitmap(bitmap)
			GlobalScope.launch(Main) {
				file?.let {
					shareFile(context, file, "image/png")
				}
			}
		}
	}

	private fun saveBitmap(bitmap: Bitmap): File? {
		return try {
			val file = File(
				context.externalCacheDir,
				"shared_barcode.png"
			)
			val fos = FileOutputStream(file)
			bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
			fos.close()
			file
		} catch (e: IOException) {
			null
		}
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
			fragment.arguments = args
			return fragment
		}
	}
}
