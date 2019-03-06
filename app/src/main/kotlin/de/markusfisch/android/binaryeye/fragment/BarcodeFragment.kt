package de.markusfisch.android.binaryeye.fragment

import com.google.zxing.BarcodeFormat

import de.markusfisch.android.binaryeye.zxing.Zxing
import de.markusfisch.android.binaryeye.BuildConfig
import de.markusfisch.android.binaryeye.R

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.graphics.Bitmap
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
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
					activity,
					e.message,
					Toast.LENGTH_SHORT
				).show()
				fragmentManager.popBackStack()
				return null
			}
			view.findViewById<ImageView>(R.id.barcode).setImageBitmap(bitmap)
			view.findViewById<View>(R.id.share).setOnClickListener { _ ->
				bitmap?.let {
					share(bitmap)
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
					val intent = Intent(Intent.ACTION_SEND)
					intent.putExtra(Intent.EXTRA_STREAM, getUriForFile(file))
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
					intent.setType("image/png")
					startActivity(intent)
				}
			}
		}
	}

	private fun saveBitmap(bitmap: Bitmap): File? {
		try {
			val file = File(
				context.getExternalCacheDir(),
				"shared_barcode.png"
			)
			val fos = FileOutputStream(file)
			bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
			fos.close()
			return file
		} catch (e: IOException) {
			return null
		}
	}

	private fun getUriForFile(file: File): Uri {
		return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			Uri.fromFile(file)
		} else {
			FileProvider.getUriForFile(
				context,
				BuildConfig.APPLICATION_ID + ".provider",
				file
			)
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
			fragment.setArguments(args)
			return fragment
		}
	}
}
