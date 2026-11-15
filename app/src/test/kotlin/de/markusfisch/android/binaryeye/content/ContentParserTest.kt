package de.markusfisch.android.binaryeye.content

import android.content.Context
import de.markusfisch.android.binaryeye.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ContentParserTest {
	private val context: Context = RuntimeEnvironment.getApplication()
	private val plain = "01095060001343521726123110LOT-7\u001d21SERIAL-9"
	private val hri = "(01)09506000134352(17)261231(10)LOT-7(21)SERIAL-9"

	@Test
	fun parsesGs1RawBytesWithoutChangingDisplayText() {
		val parsed = parseData(context, hri, plain.toByteArray(), "DataMatrix")

		assertEquals(ParsedContentType.GS1_DATA, parsed.type)
		assertEquals("LOT-7", parsed.fields.first { it.key == R.string.gs1_batch }.value)
	}

	@Test
	fun prefersRawBytesOverAmbiguousHumanReadableValues() {
		val raw = plain.replace("LOT-7", "LOT(21)ABC").toByteArray()
		val text = hri.replace("LOT-7", "LOT(21)ABC")
		val parsed = parseData(context, text, raw, "DataMatrix")

		assertEquals(ParsedContentType.GS1_DATA, parsed.type)
		assertEquals("LOT(21)ABC", parsed.fields.first { it.key == R.string.gs1_batch }.value)
		assertEquals("SERIAL-9", parsed.fields.first { it.key == R.string.gs1_serial }.value)
	}

	@Test
	fun fallsBackToTextWithoutRawBytes() {
		val parsed = parseData(context, hri, ByteArray(0), "DataMatrix")

		assertEquals(ParsedContentType.GS1_DATA, parsed.type)
	}

	@Test
	fun usesEditedBytesInsteadOfOriginalScanData() {
		val edited = hri.replace("LOT-7", "EDITED")
		val parsed = parseData(context, edited, edited.toByteArray(), "DataMatrix")

		assertEquals("EDITED", parsed.fields.first { it.key == R.string.gs1_batch }.value)
	}

	@Test
	fun preservesOtherContentParsers() {
		val parsed = parseData(context, "{}", plain.toByteArray(), "QRCode")

		assertEquals(ParsedContentType.JSON, parsed.type)
	}
}
