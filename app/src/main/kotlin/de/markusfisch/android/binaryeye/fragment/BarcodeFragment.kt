package de.markusfisch.android.binaryeye.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.EditText
import com.google.zxing.BarcodeFormat
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.*
import de.markusfisch.android.binaryeye.view.doOnApplyWindowInsets
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.ConfinedScalingImageView
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.binaryeye.zxing.Zxing
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BarcodeFragment : Fragment() {
	private enum class FileType {
		PNG, SVG
	}

	private var barcodeBitmap: Bitmap? = null
	private var barcodeSvg: String? = null
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
		val ac = activity ?: return null
		ac.setTitle(R.string.view_barcode)

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
			barcodeBitmap = Zxing.encodeAsBitmap(content, format, size, size)
			barcodeSvg = Zxing.encodeAsSvg(content, format, size, size)
		} catch (e: Exception) {
			var message = e.message
			if (message == null || message.isEmpty()) {
				message = getString(R.string.error_encoding_barcode)
			}
			message?.let {
				ac.toast(message)
			}
			fragmentManager.popBackStack()
			return null
		}
		this.content = content
		this.format = format

		val imageView = view.findViewById<ConfinedScalingImageView>(
			R.id.barcode
		)
		imageView.setImageBitmap(barcodeBitmap)
		imageView.post {
			// make sure to invoke this after ScalingImageView.onLayout()
			imageView.minWidth /= 2f
		}

		view.findViewById<View>(R.id.share).setOnClickListener {
			val bitmap = barcodeBitmap
			bitmap?.let {
				share(bitmap)
			}
		}

		(view.findViewById(R.id.inset_layout) as View).setPaddingFromWindowInsets()
		imageView.doOnApplyWindowInsets { v, insets ->
			(v as ConfinedScalingImageView).insets.set(insets)
		}

		return view
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_barcode, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.export_svg -> {
				askForFileNameAndSave(FileType.SVG)
				true
			}
			R.id.export_png -> {
				askForFileNameAndSave(FileType.PNG)
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	// dialogs do not have a parent view
	@SuppressLint("InflateParams")
	private fun askForFileNameAndSave(fileType: FileType) {
		val ac = activity ?: return
		if (!hasWritePermission(ac)) {
			return
		}
		val view = ac.layoutInflater.inflate(R.layout.dialog_save_file, null)
		val editText = view.findViewById<EditText>(R.id.file_name)
		editText.setText(encodeFileName("${format.toString()}_$content"))
		AlertDialog.Builder(ac)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val fileName = editText.text.toString()
				when (fileType) {
					FileType.PNG -> saveAs(
						addSuffixIfNotGiven(fileName, ".png"),
						"image/png"
					) {
						barcodeBitmap?.saveAsPng(it)
					}
					FileType.SVG -> saveAs(
						addSuffixIfNotGiven(fileName, ".svg"),
						"image/svg+xmg"
					) { outputStream ->
						barcodeSvg?.let {
							outputStream.write(it.toByteArray())
						}
					}
				}
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
	}

	private fun saveAs(
		fileName: String,
		mimeType: String,
		write: (outputStream: OutputStream) -> Unit
	) {
		val ac = activity ?: return
		GlobalScope.launch {
			val message = writeExternalFile(ac, fileName, mimeType, write).toSaveResult()
			GlobalScope.launch(Main) {
				ac.toast(message)
			}
		}
	}

	private fun share(bitmap: Bitmap) {
		GlobalScope.launch {
			val file = File(
				context.externalCacheDir,
				"shared_barcode.png"
			)
			val success = try {
				FileOutputStream(file).use {
					bitmap.saveAsPng(it)
				}
				true
			} catch (e: IOException) {
				false
			}
			GlobalScope.launch(Main) {
				if (success) {
					shareFile(context, file, "image/png")
				} else {
					activity?.toast(R.string.error_saving_file)
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

private fun Bitmap.saveAsPng(outputStream: OutputStream, quality: Int = 90) {
	this.compress(Bitmap.CompressFormat.PNG, quality, outputStream)
}

private val fileNameCharacters = "[^A-Za-z0-9]".toRegex()
private fun encodeFileName(name: String): String =
	fileNameCharacters.replace(name, "_").take(16).trim('_').toLowerCase(Locale.getDefault())
