package de.markusfisch.android.binaryeye.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.content.copyToClipboard
import de.markusfisch.android.binaryeye.content.shareFile
import de.markusfisch.android.binaryeye.content.shareText
import de.markusfisch.android.binaryeye.database.toScan
import de.markusfisch.android.binaryeye.graphics.COLOR_BLACK
import de.markusfisch.android.binaryeye.graphics.COLOR_WHITE
import de.markusfisch.android.binaryeye.io.addSuffixIfNotGiven
import de.markusfisch.android.binaryeye.io.toSaveResult
import de.markusfisch.android.binaryeye.io.writeExternalFile
import de.markusfisch.android.binaryeye.os.getScreenBrightness
import de.markusfisch.android.binaryeye.os.setScreenBrightness
import de.markusfisch.android.binaryeye.view.doOnApplyWindowInsets
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.ConfinedScalingImageView
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.zxingcpp.ZxingCpp
import de.markusfisch.android.zxingcpp.ZxingCpp.BarcodeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Locale
import kotlin.math.min

class BarcodeFragment : Fragment() {
	private enum class FileType {
		PNG, JPG, SVG, TXT
	}

	private val parentJob = Job()
	private val scope = CoroutineScope(Dispatchers.IO + parentJob)

	private lateinit var barcode: Barcode<*>
	private lateinit var addToHistoryItem: MenuItem
	private lateinit var brightenScreenItem: MenuItem

	private var currentBrightness = -1f;

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

		val bitmap: Bitmap
		try {
			barcode = arguments?.toBarcode() ?: throw IllegalArgumentException(
				"Illegal arguments"
			)
			// Catch exceptions from encoding.
			bitmap = barcode.bitmap()
		} catch (e: Exception) {
			var message = e.message
			if (message.isNullOrEmpty()) {
				message = getString(R.string.error_encoding_barcode)
			}
			message?.let {
				ac.toast(message)
			}
			fragmentManager.popBackStack()
			return null
		}

		val imageView = view.findViewById<ConfinedScalingImageView>(
			R.id.barcode
		)
		imageView.setImageBitmap(bitmap)
		imageView.post {
			// Make sure to invoke this after ScalingImageView.onLayout().
			imageView.minWidth = min(
				imageView.minWidth / 2f,
				barcode.size.toFloat()
			)
		}

		view.findViewById<View>(R.id.share).setOnClickListener {
			it.context?.apply {
				pickFileType(R.string.share_as) { fileType ->
					shareAs(fileType)
				}
			}
		}

		(view.findViewById(R.id.inset_layout) as View).setPaddingFromWindowInsets()
		imageView.doOnApplyWindowInsets { v, insets ->
			(v as ConfinedScalingImageView).insets.set(insets)
		}

		return view
	}

	private fun Bundle.toBarcode() = Barcode(
		getString(CONTENT_TEXT) ?: getByteArray(CONTENT_RAW) ?: throw IllegalArgumentException(
			"content cannot be null"
		),
		BarcodeFormat.valueOf(
			getString(FORMAT) ?: throw IllegalArgumentException(
				"format cannot be null"
			)
		),
		getInt(SIZE),
		getInt(MARGIN),
		getInt(EC_LEVEL),
		Colors.entries[getInt(COLORS)]
	)

	override fun onResume() {
		super.onResume()
		if (prefs.brightenScreen) {
			// Post to make sure brightenScreenItem is initialized.
			view?.post {
				brightenScreen()
			}
		}
	}

	override fun onPause() {
		super.onPause()
		if (currentBrightness > -1f) {
			restoreScreenBrightness()
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		parentJob.cancel()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_barcode, menu)
		addToHistoryItem = menu.findItem(R.id.add_to_history)
		brightenScreenItem = menu.findItem(R.id.brighten_screen)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.add_to_history -> {
				readAndAddToHistory(barcode.bitmap(), barcode.format)
				addToHistoryItem.isVisible = false
				context.toast(R.string.added_to_history)
				true
			}

			R.id.copy_to_clipboard -> {
				context.apply {
					copyToClipboard(barcode.text())
					toast(R.string.copied_to_clipboard)
				}
				true
			}

			R.id.export_to_file -> {
				context.pickFileType(R.string.export_as) {
					askForFileNameAndSave(it)
				}
				true
			}

			R.id.brighten_screen -> {
				prefs.brightenScreen = if (currentBrightness > -1f) {
					restoreScreenBrightness()
					false
				} else {
					brightenScreen()
					true
				}
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun Context.pickFileType(
		title: Int,
		action: (FileType) -> Unit
	) {
		val fileTypes = FileType.entries.toTypedArray()
		AlertDialog.Builder(this)
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
		editText.setText(
			encodeFileName("${barcode.format}_${barcode.content}")
		)
		AlertDialog.Builder(ac)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val fileName = editText.text.toString()
				when (fileType) {
					FileType.PNG -> saveAs(
						addSuffixIfNotGiven(fileName, ".png"),
						MIME_PNG
					) {
						barcode.bitmap().saveAsPng(it)
					}

					FileType.JPG -> saveAs(
						addSuffixIfNotGiven(fileName, ".jpg"),
						MIME_JPG
					) {
						barcode.bitmap().saveAsJpg(it)
					}

					FileType.SVG -> saveAs(
						addSuffixIfNotGiven(fileName, ".svg"),
						MIME_SVG
					) { outputStream ->
						outputStream.write(barcode.svg().toByteArray())
					}

					FileType.TXT -> saveAs(
						addSuffixIfNotGiven(fileName, ".txt"),
						MIME_TXT
					) { outputStream ->
						outputStream.write(barcode.text().toByteArray())
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
		scope.launch(Dispatchers.IO) {
			val message = ac.writeExternalFile(fileName, mimeType, write).toSaveResult()
			withContext(Dispatchers.Main) {
				ac.toast(message)
			}
		}
	}

	private fun Context.shareAs(fileType: FileType) {
		when (fileType) {
			FileType.PNG -> share(barcode.bitmap(), MIME_PNG, "png")
			FileType.JPG -> share(barcode.bitmap(), MIME_JPG, "jpg")
			FileType.SVG -> shareText(barcode.svg(), MIME_SVG)
			FileType.TXT -> shareText(barcode.text())
		}
	}

	private fun Context.share(bitmap: Bitmap, mimeType: String, ext: String) {
		scope.launch(Dispatchers.IO) {
			val file = File(
				externalCacheDir,
				"shared_barcode.$ext"
			)
			val success = try {
				FileOutputStream(file).use {
					when (mimeType) {
						MIME_PNG -> bitmap.saveAsPng(it)
						MIME_JPG -> bitmap.saveAsJpg(it)
						else -> throw IllegalArgumentException(
							"Invalid mime type"
						)
					}
				}
				true
			} catch (e: IOException) {
				false
			}
			withContext(Dispatchers.Main) {
				if (success) {
					shareFile(file, mimeType)
				} else {
					toast(R.string.error_saving_file)
				}
			}
		}
	}

	private fun brightenScreen() {
		activity?.let {
			currentBrightness = it.getScreenBrightness()
			it.setScreenBrightness(1f);
			brightenScreenItem.isChecked = true
		}
	}

	private fun restoreScreenBrightness() {
		if (currentBrightness > -1f) {
			activity?.setScreenBrightness(currentBrightness);
			currentBrightness = -1f
			brightenScreenItem.isChecked = false
		}
	}

	companion object {
		private const val CONTENT_TEXT = "content_text"
		private const val CONTENT_RAW = "content_raw"
		private const val FORMAT = "format"
		private const val SIZE = "size"
		private const val MARGIN = "margin"
		private const val EC_LEVEL = "ec_level"
		private const val COLORS = "colors"
		private const val MIME_PNG = "image/png"
		private const val MIME_JPG = "image/jpeg"
		private const val MIME_SVG = "image/svg+xmg"
		private const val MIME_TXT = "text/plain"

		fun <T> newInstance(
			content: T,
			format: BarcodeFormat,
			size: Int,
			margin: Int,
			ecLevel: Int = -1,
			colors: Int = 0
		): Fragment {
			val args = Bundle()
			when (content) {
				is String -> {
					args.putString(CONTENT_TEXT, content)
				}

				is ByteArray -> {
					args.putByteArray(CONTENT_RAW, content)
				}

				else -> {
					throw IllegalArgumentException(
						"content must be a String of a ByteArray"
					)
				}
			}
			args.putString(FORMAT, format.name)
			args.putInt(SIZE, size)
			args.putInt(MARGIN, margin)
			args.putInt(EC_LEVEL, ecLevel)
			args.putInt(COLORS, colors)
			val fragment = BarcodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}

private data class Barcode<T>(
	val content: T,
	val format: BarcodeFormat,
	val size: Int,
	val margin: Int,
	val ecLevel: Int,
	val colors: Colors
) {
	private var _bitmap: Bitmap? = null
	fun bitmap(): Bitmap {
		val b = _bitmap ?: ZxingCpp.encodeAsBitmap(
			content, format, size, size, margin, ecLevel,
			setColor = colors.foregroundColor(),
			unsetColor = colors.backgroundColor()
		)
		_bitmap = b
		return b
	}

	private var _svg: String? = null
	fun svg(): String {
		val s = _svg ?: ZxingCpp.encodeAsSvg(
			content, format, margin, ecLevel
		)
		_svg = s
		return s
	}

	private var _text: String? = null
	fun text(): String {
		val t = _text ?: ZxingCpp.encodeAsText(
			content, format, margin, ecLevel,
			inverted = colors == Colors.BLACK_ON_WHITE
		)
		_text = t
		return t
	}
}

private enum class Colors {
	BLACK_ON_WHITE,
	WHITE_ON_BLACK,
	BLACK_ON_TRANSPARENT,
	WHITE_ON_TRANSPARENT;

	fun foregroundColor(): Int = when (this) {
		BLACK_ON_WHITE,
		BLACK_ON_TRANSPARENT -> COLOR_BLACK

		WHITE_ON_BLACK,
		WHITE_ON_TRANSPARENT -> COLOR_WHITE
	}

	fun backgroundColor(): Int = when (this) {
		BLACK_ON_WHITE -> COLOR_WHITE
		WHITE_ON_BLACK -> COLOR_BLACK
		BLACK_ON_TRANSPARENT,
		WHITE_ON_TRANSPARENT -> 0
	}
}

private fun Bitmap.saveAsPng(outputStream: OutputStream, quality: Int = 90) =
	compress(Bitmap.CompressFormat.PNG, quality, outputStream)

private fun Bitmap.saveAsJpg(outputStream: OutputStream, quality: Int = 90) =
	compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

private val fileNameCharacters = "[^A-Za-z0-9]".toRegex()
private fun encodeFileName(name: String): String = fileNameCharacters
	.replace(name, "_")
	.take(16)
	.trim('_')
	.lowercase(Locale.getDefault())

private fun readAndAddToHistory(
	bitmap: Bitmap,
	format: BarcodeFormat
) {
	ZxingCpp.readBitmap(
		bitmap,
		0, 0,
		bitmap.width, bitmap.height,
		0,
		ZxingCpp.ReaderOptions(
			tryHarder = true,
			tryRotate = true,
			tryInvert = true,
			tryDownscale = true,
			maxNumberOfSymbols = 1,
			formats = setOf(format)
		)
	)?.first {
		db.insertScan(it.toScan())
		true
	}
}
