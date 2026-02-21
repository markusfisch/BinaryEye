package de.markusfisch.android.binaryeye.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
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
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.db
import de.markusfisch.android.binaryeye.app.hasWritePermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.content.Barcode
import de.markusfisch.android.binaryeye.content.BarcodeColors
import de.markusfisch.android.binaryeye.content.BitMatrixBarcode
import de.markusfisch.android.binaryeye.content.ContentBarcode
import de.markusfisch.android.binaryeye.content.copyToClipboard
import de.markusfisch.android.binaryeye.content.shareFile
import de.markusfisch.android.binaryeye.content.shareText
import de.markusfisch.android.binaryeye.database.toScan
import de.markusfisch.android.binaryeye.io.addSuffixIfNotGiven
import de.markusfisch.android.binaryeye.io.toSaveResult
import de.markusfisch.android.binaryeye.io.writeExternalFile
import de.markusfisch.android.binaryeye.net.createEncodeDeeplink
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
import kotlin.math.max
import kotlin.math.min

class BarcodeFragment : Fragment() {
	private enum class FileType {
		PNG, JPG, SVG, TXT
	}

	private val parentJob = Job()
	private val scope = CoroutineScope(Dispatchers.IO + parentJob)

	private lateinit var imageView: ConfinedScalingImageView
	private lateinit var barcode: Barcode<*>
	private lateinit var addToHistoryItem: MenuItem
	private lateinit var brightenScreenItem: MenuItem

	private var currentBrightness = -1f

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

		// Make `imageView` available for `onPause()`.
		imageView = view.findViewById(R.id.barcode)

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

		imageView.setImageBitmap(bitmap)
		imageView.runAfterLayout = {
			val scale = prefs.previewScale
			if (scale > 0) {
				imageView.setScale(
					scale,
					bitmap.width / 2f,
					bitmap.height / 2f
				)
			}

			imageView.minWidth = min(
				imageView.minWidth / 2f,
				max(bitmap.width, bitmap.height).toFloat()
			)
		}

		view.findViewById<View>(R.id.share).setOnClickListener {
			it.context?.apply {
				pickFileType(R.string.share_as) { fileType ->
					shareAs(fileType)
				}
			}
		}

		view.findViewById<View>(R.id.inset_layout).setPaddingFromWindowInsets()
		imageView.doOnApplyWindowInsets { _, insets ->
			imageView.insets.set(insets)
		}

		return view
	}

	private fun Bundle.toBarcode(): Barcode<*> {
		val content =
			getString(CONTENT_TEXT) ?: getByteArray(CONTENT_RAW) ?: throw IllegalArgumentException(
				"content cannot be null"
			)
		val barcodeFormat = BarcodeFormat.valueOf(
			getString(FORMAT) ?: throw IllegalArgumentException(
				"format cannot be null"
			)
		)
		val colors = BarcodeColors.entries[getInt(COLORS)]
		val data = getByteArray(BIT_MATRIX_DATA)
		return if (data != null) {
			BitMatrixBarcode(
				ZxingCpp.BitMatrix(
					getInt(BIT_MATRIX_WIDTH),
					getInt(BIT_MATRIX_HEIGHT),
					data
				),
				content,
				barcodeFormat,
				colors
			)
		} else {
			ContentBarcode(
				content,
				barcodeFormat,
				getInt(EC_LEVEL),
				getInt(MARGIN),
				colors
			)
		}
	}

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
		if (imageView.isInBounds()) {
			prefs.previewScale = imageView.getScale()
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
				readAndAddToHistory(
					barcode.bitmap(),
					barcode.format
				)
				addToHistoryItem.isVisible = false
				context.toast(R.string.added_to_history)
				true
			}

			R.id.copy_to_clipboard -> {
				copyToClipboard(barcode.text())
				true
			}

			R.id.copy_as_deeplink -> {
				copyToClipboard(deeplinkToCopy())
				true
			}

			R.id.export_to_file -> {
				context.pickFileType(R.string.export_as) { fileType ->
					when (fileType) {
						FileType.PNG,
						FileType.JPG -> askForSize { size ->
							askForFileNameAndSave(fileType, size)
						}

						else -> askForFileNameAndSave(fileType, 0)
					}
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

	private fun copyToClipboard(text: String, isSensitive: Boolean = false) {
		activity?.apply {
			copyToClipboard(text, isSensitive)
			toast(R.string.copied_to_clipboard)
		}
	}

	private fun deeplinkToCopy() = createEncodeDeeplink(
		format = barcode.format.name,
		content = barcode.textOrHex(),
	)

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
	private fun askForFileNameAndSave(fileType: FileType, bitmapSize: Int) {
		val ac = activity ?: return
		// Write permission is only required before Android Q.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
			!ac.hasWritePermission {
				askForFileNameAndSave(fileType, bitmapSize)
			}
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
						barcode.bitmap(bitmapSize).saveAsPng(it)
					}

					FileType.JPG -> saveAs(
						addSuffixIfNotGiven(fileName, ".jpg"),
						MIME_JPG
					) {
						barcode.bitmap(bitmapSize).saveAsJpg(it)
					}

					FileType.SVG -> saveAs(
						addSuffixIfNotGiven(fileName, ".svg"),
						MIME_SVG
					) {
						it.write(barcode.svg().toByteArray())
					}

					FileType.TXT -> saveAs(
						addSuffixIfNotGiven(fileName, ".txt"),
						MIME_TXT
					) {
						it.write(barcode.text().toByteArray())
					}
				}
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
	}

	// Dialogs do not have a parent view.
	@SuppressLint("InflateParams")
	private fun askForSize(write: (size: Int) -> Unit) {
		val ac = activity ?: return
		val view = ac.layoutInflater.inflate(R.layout.dialog_size, null)
		val sizeView = view.findViewById<TextView>(R.id.size_display)
		val sizeBarView = view.findViewById<SeekBar>(R.id.size_bar)
		sizeBarView.initSizeBar(sizeView)
		AlertDialog.Builder(ac)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				write(getSize(sizeBarView.progress))
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
	}

	private fun SeekBar.initSizeBar(sizeView: TextView) {
		sizeView.updateSize(progress)
		setOnSeekBarChangeListener(
			object : SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(
					seekBar: SeekBar,
					progressValue: Int,
					fromUser: Boolean
				) {
					sizeView.updateSize(progressValue)
				}

				override fun onStartTrackingTouch(seekBar: SeekBar) {}

				override fun onStopTrackingTouch(seekBar: SeekBar) {}
			}
		)
	}

	private fun TextView.updateSize(power: Int) {
		val size = getSize(power)
		text = if (size > 0) {
			getString(R.string.size_width_by_height, size, size)
		} else {
			getString(R.string.size_no_magnification)
		}
	}

	private fun saveAs(
		fileName: String,
		mimeType: String,
		write: (outputStream: OutputStream) -> Unit
	) {
		val ac = activity ?: return
		scope.launch(Dispatchers.IO) {
			val message = ac.writeExternalFile(
				fileName, mimeType, write
			).toSaveResult()
			withContext(Dispatchers.Main) {
				ac.toast(message)
			}
		}
	}

	private fun Context.shareAs(fileType: FileType) {
		when (fileType) {
			FileType.PNG -> askForSize {
				share(barcode.bitmap(it), MIME_PNG, "png")
			}

			FileType.JPG -> askForSize {
				share(barcode.bitmap(it), MIME_JPG, "jpg")
			}

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
			} catch (_: IOException) {
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
			if (!isAdded) {
				return
			}
			currentBrightness = it.getScreenBrightness()
			it.setScreenBrightness(1f)
			brightenScreenItem.isChecked = true
		}
	}

	private fun restoreScreenBrightness() {
		if (currentBrightness > -1f && isAdded) {
			activity?.setScreenBrightness(currentBrightness)
			currentBrightness = -1f
			brightenScreenItem.isChecked = false
		}
	}

	companion object {
		private const val BIT_MATRIX_WIDTH = "bit_matrix_width"
		private const val BIT_MATRIX_HEIGHT = "bit_matrix_height"
		private const val BIT_MATRIX_DATA = "bit_matrix_data"
		private const val CONTENT_TEXT = "content_text"
		private const val CONTENT_RAW = "content_raw"
		private const val FORMAT = "format"
		private const val EC_LEVEL = "ec_level"
		private const val MARGIN = "margin"
		private const val COLORS = "colors"
		private const val MIME_PNG = "image/png"
		private const val MIME_JPG = "image/jpeg"
		private const val MIME_SVG = "image/svg+xmg"
		private const val MIME_TXT = "text/plain"

		fun newInstance(
			barcode: Barcode<*>,
			colors: Int = 0
		): Fragment {
			val args = Bundle()
			when (barcode) {
				is ContentBarcode -> args.putEcLevelMargin(
					barcode.ecLevel,
					barcode.margin
				)

				is BitMatrixBarcode -> {
					args.putInt(BIT_MATRIX_WIDTH, barcode.bitMatrix.width)
					args.putInt(BIT_MATRIX_HEIGHT, barcode.bitMatrix.height)
					args.putByteArray(BIT_MATRIX_DATA, barcode.bitMatrix.data)
				}

				else -> throw IllegalArgumentException("unknown barcode")
			}
			args.putContentFormatColors(
				barcode.content,
				barcode.format,
				colors
			)
			val fragment = BarcodeFragment()
			fragment.arguments = args
			return fragment
		}

		fun <T> newInstance(
			content: T,
			format: BarcodeFormat,
			ecLevel: Int = -1,
			margin: Int = -1,
			colors: Int = 0
		): Fragment {
			val args = Bundle()
			args.putContentFormatColors(content, format, colors)
			args.putEcLevelMargin(ecLevel, margin)
			val fragment = BarcodeFragment()
			fragment.arguments = args
			return fragment
		}

		private fun <T> Bundle.putContentFormatColors(
			content: T,
			format: BarcodeFormat,
			colors: Int
		) {
			when (content) {
				is String -> {
					putString(CONTENT_TEXT, content)
				}

				is ByteArray -> {
					putByteArray(CONTENT_RAW, content)
				}

				else -> {
					throw IllegalArgumentException(
						"content must be a String of a ByteArray"
					)
				}
			}
			putString(FORMAT, format.name)
			putInt(COLORS, colors)
		}

		private fun Bundle.putEcLevelMargin(
			ecLevel: Int,
			margin: Int
		) {
			putInt(EC_LEVEL, ecLevel)
			putInt(MARGIN, margin)
		}
	}
}

private fun ImageView.setScale(scale: Float, x: Float, y: Float) {
	imageMatrix = Matrix().apply {
		setTranslate(-x, -y)
		postScale(scale, scale)
		postTranslate(x, y)
	}
}

private fun ImageView.getScale(): Float {
	val values = FloatArray(9)
	imageMatrix.getValues(values)
	return values[Matrix.MSCALE_X]
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

private fun getSize(step: Int) = when (step) {
	0 -> 0
	else -> 128 shl (step - 1)
}

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
