package de.markusfisch.android.binaryeye.adapter

import android.content.Context
import android.database.MatrixCursor
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.content.ParsedContentType
import de.markusfisch.android.binaryeye.database.Database
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ScansAdapterTest {
	private val context: Context = RuntimeEnvironment.getApplication()
	private val plain = "01095060001343521726123110LOT-7\u001d21SERIAL-9"
	private val hri = "(01)09506000134352(17)261231(10)LOT-7(21)SERIAL-9"

	@Test
	fun showsGtinForGs1TextAndPreservesStoredContent() {
		for (text in listOf(plain, hri)) {
			cursor(text, ParsedContentType.GS1_DATA).use { cursor ->
				val adapter = ScansAdapter(context, cursor) { _, _ -> }
				val view = bind(adapter, cursor)

				assertEquals("09506000134352", view.findViewById<TextView>(R.id.content).text.toString())
				assertEquals(text, adapter.getContent(0))
			}
		}
	}

	@Test
	fun recognizesGs1InLegacyUnknownHistoryEntries() {
		for (text in listOf(plain, hri)) {
			cursor(text, ParsedContentType.UNKNOWN).use { cursor ->
				val adapter = ScansAdapter(context, cursor) { _, _ -> }
				val view = bind(adapter, cursor)

				assertEquals("09506000134352", view.findViewById<TextView>(R.id.content).text.toString())
				assertEquals(
					context.getString(R.string.scan_meta_data, "Data Matrix", context.getString(R.string.parsed_type_gs1_data)),
					view.findViewById<TextView>(R.id.format).text.toString()
				)
				assertEquals(text, adapter.getContent(0))
			}
		}
	}

	@Test
	fun keepsLabelsAheadOfGs1Preview() {
		for (type in listOf(ParsedContentType.GS1_DATA, ParsedContentType.UNKNOWN)) {
			cursor(hri, type, "My medicine").use { cursor ->
				val adapter = ScansAdapter(context, cursor) { _, _ -> }
				val view = bind(adapter, cursor)

				assertEquals("My medicine", view.findViewById<TextView>(R.id.content).text.toString())
			}
		}
	}

	@Test
	fun preservesOtherTextAndBinaryPreviews() {
		for ((text, raw, expected) in listOf(
			Triple("https://example.com/path", ByteArray(0), "https://example.com/path"),
			Triple("", byteArrayOf(0, 1, 2), "000102")
		)) {
			cursor(text, ParsedContentType.UNKNOWN, raw = raw).use { cursor ->
				val adapter = ScansAdapter(context, cursor) { _, _ -> }
				val view = bind(adapter, cursor)

				assertEquals(expected, view.findViewById<TextView>(R.id.content).text.toString())
			}
		}
	}

	private fun bind(adapter: ScansAdapter, cursor: MatrixCursor): View {
		cursor.moveToFirst()
		val view = adapter.newView(context, cursor, FrameLayout(context))!!
		adapter.bindView(view, context, cursor)
		return view
	}

	private fun cursor(
		text: String,
		type: ParsedContentType,
		name: String? = null,
		raw: ByteArray = plain.toByteArray()
	) = MatrixCursor(arrayOf(
		Database.SCANS_ID,
		Database.SCANS_PINNED,
		Database.SCANS_DATETIME,
		Database.SCANS_NAME,
		Database.SCANS_TEXT,
		Database.SCANS_RAW,
		Database.SCANS_CONTENT_TYPE,
		Database.SCANS_FORMAT
	)).apply {
		addRow(arrayOf(1L, 0, "2026-11-15 12:00:00", name, text, raw, type.name, "DataMatrix"))
	}
}
