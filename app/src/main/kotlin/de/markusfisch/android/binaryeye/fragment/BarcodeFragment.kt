package de.markusfisch.android.binaryeye.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.EditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.content.copyToClipboard
import de.markusfisch.android.binaryeye.content.shareFile
import de.markusfisch.android.binaryeye.content.shareText
import de.markusfisch.android.binaryeye.io.addSuffixIfNotGiven
import de.markusfisch.android.binaryeye.io.toSaveResult
import de.markusfisch.android.binaryeye.io.writeExternalFile
import de.markusfisch.android.binaryeye.view.doOnApplyWindowInsets
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.ConfinedScalingImageView
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.binaryeye.zxing.encodeAsBitmap
import de.markusfisch.android.binaryeye.zxing.encodeAsSvg
import de.markusfisch.android.binaryeye.zxing.encodeAsText
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BarcodeFragment : Fragment() {
	private enum class FileType {
		PNG, SVG, TXT
	}

	private val parentJob = Job()
	private val scope = CoroutineScope(Dispatchers.IO + parentJob)

	private var barcodeBitmap: Bitmap? = null
	private var barcodeSvg: String? = null
	private var barcodeTxt: String? = null
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
		val hints = args.getSerializable(HINTS) as EnumMap<EncodeHintType, Any>?
		val size = args.getInt(SIZE)
		try {
			barcodeBitmap = encodeAsBitmap(content, format, size, size, hints)
			barcodeSvg = encodeAsSvg(content, format, hints)
			barcodeTxt = encodeAsText(content, format, hints)
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
			// Make sure to invoke this after ScalingImageView.onLayout().
			imageView.minWidth /= 2f
		}

		view.findViewById<View>(R.id.share).setOnClickListener {
			pickFileType(context, R.string.share_as) {
				shareAs(it)
			}
		}

		(view.findViewById(R.id.inset_layout) as View).setPaddingFromWindowInsets()
		imageView.doOnApplyWindowInsets { v, insets ->
			(v as ConfinedScalingImageView).insets.set(insets)
		}

		return view
	}

	override fun onDestroyView() {
		super.onDestroyView()
		parentJob.cancel()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_barcode, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.copy_to_clipboard -> {
				barcodeTxt?.let {
					context.copyToClipboard(it)
					context.toast(R.string.copied_to_clipboard)
				}
				true
			}
			R.id.export_to_file -> {
				pickFileType(context, R.string.export_as) {
					askForFileNameAndSave(it)
				}
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun pickFileType(
		context: Context,
		title: Int,
		action: (FileType) -> Unit
	) {
		val fileTypes = FileType.values()
		AlertDialog.Builder(context)
			.setTitle(title)
			.setItems(fileTypes.map { it.name }.toTypedArray()) { _, which ->
				action(fileTypes[which])
			}
			.show()
	}

	// Dialogs do not have a parent view.
	@SuppressLint("InflateParams")
	private fun askForFileNameAndSave(fileType: FileType) {
		val ac = activity ?: return
		// Write permission is only required before Android Q.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
			!ac.hasWritePermission { askForFileNameAndSave(fileType) }
		) {
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
						MIME_PNG
					) {
						barcodeBitmap?.saveAsPng(it)
					}
					FileType.SVG -> saveAs(
						addSuffixIfNotGiven(fileName, ".svg"),
						MIME_SVG
					) { outputStream ->
						barcodeSvg?.let {
							outputStream.write(it.toByteArray())
						}
					}
					FileType.TXT -> saveAs(
						addSuffixIfNotGiven(fileName, ".txt"),
						MIME_TXT
					) { outputStream ->
						barcodeTxt?.let {
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
		GlobalScope.launch(Dispatchers.IO) {
			val message = writeExternalFile(ac, fileName, mimeType, write).toSaveResult()
			launch(Dispatchers.Main) {
				ac.toast(message)
			}
		}
	}

	private fun shareAs(fileType: FileType) {
		when (fileType) {
			FileType.PNG -> barcodeBitmap?.let { share(it) }
			FileType.SVG -> barcodeSvg?.let { context.shareText(it, MIME_SVG) }
			FileType.TXT -> barcodeTxt?.let { context.shareText(it) }
		}
	}

	private fun share(bitmap: Bitmap) {
		val ctx = context ?: return
		GlobalScope.launch(Dispatchers.IO) {
			val file = File(
				ctx.externalCacheDir,
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
			launch(Dispatchers.Main) {
				if (success) {
					ctx.shareFile(file, "image/png")
				} else {
					ctx.toast(R.string.error_saving_file)
				}
			}
		}
	}

	companion object {
		private const val CONTENT = "content"
		private const val FORMAT = "format"
		private const val HINTS = "hints"
		private const val SIZE = "size"
		private const val MIME_PNG = "image/png"
		private const val MIME_SVG = "image/svg+xmg"
		private const val MIME_TXT = "text/plain"

		fun newInstance(
			content: String,
			format: BarcodeFormat,
			size: Int,
			hints: EnumMap<EncodeHintType, Any>? = null
		): Fragment {
			val args = Bundle()
			args.putString(CONTENT, content)
			args.putSerializable(FORMAT, format)
			hints?.let {
				args.putSerializable(HINTS, it)
			}
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
	fileNameCharacters.replace(name, "_").take(16).trim('_').lowercase(Locale.getDefault())
