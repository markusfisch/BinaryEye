package de.markusfisch.android.binaryeye.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.view.*
import android.widget.EditText
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.addSuffixIfNotGiven
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.setWindowInsetListener
import de.markusfisch.android.binaryeye.app.shareFile
import de.markusfisch.android.binaryeye.view.setPadding
import de.markusfisch.android.binaryeye.widget.ConfinedScalingImageView
import de.markusfisch.android.binaryeye.zxing.Zxing
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class BarcodeFragment : Fragment() {
	private var barcode: Bitmap? = null
	private var content: String = ""
	private var format: BarcodeFormat? = null

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		activity?.setTitle(R.string.view_barcode)

		val view = inflater.inflate(
			R.layout.fragment_barcode,
			container,
			false
		)

		val args = arguments ?: return view
		val content = args.getString(CONTENT) ?: return view
		val format = args.getSerializable(FORMAT) as BarcodeFormat? ?: return view
		val size = args.getInt(SIZE)
		try {
			barcode = Zxing.encodeAsBitmap(content, format, size, size)
		} catch (e: Exception) {
			var message = e.message
			if (message == null || message.isEmpty()) {
				message = getString(R.string.error_encoding_barcode)
			}
			Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
			fragmentManager.popBackStack()
			return null
		}
		this.content = content
		this.format = format

		val imageView = view.findViewById<ConfinedScalingImageView>(
			R.id.barcode
		)
		imageView.setImageBitmap(barcode)
		imageView.post {
			// make sure to invoke this after ScalingImageView.onLayout()
			imageView.minWidth /= 2f
		}

		view.findViewById<View>(R.id.share).setOnClickListener {
			val bitmap = barcode
			bitmap?.let {
				share(bitmap)
			}
		}

		setWindowInsetListener { insets ->
			(view.findViewById(R.id.inset_layout) as View).setPadding(insets)
			imageView.insets.set(insets)
		}

		return view
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_barcode, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.save -> {
				askForFileNameAndSave()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	// dialogs do not have a parent view
	@SuppressLint("InflateParams")
	private fun askForFileNameAndSave() {
		val ac = activity ?: return
		val view = ac.layoutInflater.inflate(R.layout.dialog_save_file, null)
		val editText = view.findViewById<EditText>(R.id.file_name)
		editText.setText(encodeFileName("${format.toString()}_$content"))
		AlertDialog.Builder(ac)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val bitmap = barcode
				bitmap?.let {
					saveAsFile(
						bitmap,
						addSuffixIfNotGiven(
							editText.text.toString(),
							".png"
						)
					)
				}
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
	}

	private fun saveAsFile(bitmap: Bitmap, path: String) {
		val ac = activity ?: return
		if (!hasWritePermission(ac)) {
			return
		}
		GlobalScope.launch {
			val success = saveBitmap(
				bitmap,
				File(
					Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_DOWNLOADS
					),
					path
				)
			)
			GlobalScope.launch(Main) {
				Toast.makeText(
					ac,
					if (success) {
						R.string.saved_in_downloads
					} else {
						R.string.error_saving_binary_data
					},
					Toast.LENGTH_LONG
				).show()
			}
		}
	}

	private fun share(bitmap: Bitmap) {
		GlobalScope.launch {
			val file = File(
				context.externalCacheDir,
				"shared_barcode.png"
			)
			val success = saveBitmap(bitmap, file)
			GlobalScope.launch(Main) {
				if (success) {
					shareFile(context, file, "image/png")
				} else {
					val ac = activity
					ac?.let {
						Toast.makeText(
							ac,
							R.string.error_saving_binary_data,
							Toast.LENGTH_LONG
						).show()
					}
				}
			}
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

private fun saveBitmap(bitmap: Bitmap, file: File): Boolean {
	return try {
		val fos = FileOutputStream(file)
		bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
		fos.close()
		true
	} catch (e: IOException) {
		false
	}
}

private val fileNameCharacters = "[^A-Za-z0-9]".toRegex()
private fun encodeFileName(name: String): String =
	fileNameCharacters.replace(name, "_").take(16).trim('_').toLowerCase(Locale.getDefault())
