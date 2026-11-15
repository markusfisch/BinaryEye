package de.markusfisch.android.binaryeye.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Gs1MedicationParserTest {
	@Test
	fun parsesPlainGs1DataWithGroupSeparator() {
		val text = "01095060001343521726123110LOT-7\u001d21SERIAL-9"

		assertEquals(
			Gs1Medication("09506000134352", "2026-12-31", "LOT-7", "SERIAL-9"),
			parse(text)
		)
	}

	@Test
	fun parsesPlainGs1DataWithNationalReimbursementNumber() {
		val text = "010950600013435217261231710PZN-123\u001d10LOT-7\u001d21SERIAL-9"

		assertEquals(
			Gs1Medication(
				"09506000134352",
				"2026-12-31",
				"LOT-7",
				"SERIAL-9",
				mapOf("710" to "PZN-123")
			),
			parse(text)
		)
	}

	@Test
	fun parsesHumanReadableGs1Data() {
		val text = "(01)09506000134352(17)261231(10)LOT-7(21)SERIAL-9"

		assertEquals(
			Gs1Medication("09506000134352", "2026-12-31", "LOT-7", "SERIAL-9"),
			parse(text)
		)
	}

	@Test
	fun parsesSerialBeforeExpiryLikeTheScannedExample() {
		val text = "010590999138321321FXS2WSCHHPNM\u001d17300930\u001d10191097"
		val hri = "(01)05909991383213(21)FXS2WSCHHPNM(17)300930(10)191097"
		val expected = Gs1Medication(
			"05909991383213",
			"2030-09-30",
			"191097",
			"FXS2WSCHHPNM"
		)

		assertEquals(expected, parse(text))
		assertEquals(expected, parse(hri))
	}

	@Test
	fun interpretsUnspecifiedExpiryDayAsLastDayOfMonth() {
		val text = "(01)09506000134352(17)280200(10)LOT-7(21)SERIAL-9"

		assertEquals(
			Gs1Medication("09506000134352", "2028-02-29", "LOT-7", "SERIAL-9"),
			parse(text)
		)
	}

	@Test
	fun rejectsInvalidGtinAndExpiryDate() {
		assertNull(
			parse(
				"01095060001343531726123110LOT-7\u001d21SERIAL-9"
			)
		)
		assertNull(
			parse(
				"01095060001343521726023010LOT-7\u001d21SERIAL-9"
			)
		)
		assertNull(
			parse("(01)٠9506000134352(17)261231(10)LOT-7(21)SERIAL-9")
		)
	}

	@Test
	fun rejectsOtherFormatsAndIncompleteMedicationData() {
		assertNull(
			parse(
				"(01)09506000134352(17)261231(10)LOT-7(21)SERIAL-9",
				"QRCode"
			)
		)
		assertNull(
			parse(
				"(01)09506000134352(17)261231"
			)
		)
	}

	@Test
	fun resolvesTwoDigitYearsUsingGs1RollingCenturyWindow() {
		assertEquals(
			"1977-01-01",
			parse(
				"(01)09506000134352(17)770101(10)LOT-7(21)SERIAL-9",
				currentYear = 2026
			)?.expiry
		)
		assertEquals(
			"2050-01-01",
			parse(
				"(01)09506000134352(17)500101(10)LOT-7(21)SERIAL-9",
				currentYear = 2051
			)?.expiry
		)
	}

	@Test
	fun skipsAdditionalElementsWithoutLosingMedicationFields() {
		val text = "01095060001343521126010117261231" +
			"31020004004101234567890123" +
			"240PRODUCT\u001d241CUSTOMER\u001d" +
			"10LOT-7\u001d21SERIAL-9\u001d" +
			"91" + "A".repeat(30) + "\u001d710PZN-123"

		assertEquals(
			Gs1Medication(
				"09506000134352",
				"2026-12-31",
				"LOT-7",
				"SERIAL-9",
				mapOf("710" to "PZN-123")
			),
			parse(text)
		)
	}

	@Test
	fun validatesVariableFieldsInBothRepresentations() {
		for (ai in listOf("10", "21", "710", "711", "712", "713", "714", "715")) {
			for (value in listOf("", "A".repeat(21))) {
				val elements = linkedMapOf(
					"01" to "09506000134352",
					"17" to "261231",
					"10" to "LOT-7",
					"21" to "SERIAL-9"
				)
				elements[ai] = value
				val plain = elements.entries.joinToString("\u001d") { (key, data) ->
					key + data
				}
				val hri = elements.entries.joinToString("") { (key, data) ->
					"($key)$data"
				}
				assertNull("AI $ai, length ${value.length}, plain", parse(plain))
				assertNull("AI $ai, length ${value.length}, HRI", parse(hri))
			}
		}
	}

	@Test
	fun rejectsTruncatedOrSeparatedPredefinedElements() {
		val text = "01095060001343521726123110LOT-7\u001d21SERIAL-9\u001d"

		assertNull(parse(text + "112601"))
		assertNull(parse(text + "1126\u001d0101"))
	}

	@Test
	fun rejectsDuplicateMedicationElements() {
		assertNull(parse("01095060001343521726123110LOT\u001d10OTHER\u001d21SERIAL"))
		assertNull(parse("(01)09506000134352(17)261231(10)LOT(10)OTHER(21)SERIAL"))
	}

	@Test
	fun preservesLiteralParenthesesInHumanReadableValues() {
		val plain = "01095060001343521726123110LOT(A)\u001d21SER(B)\u001d710PZN(C)"
		val hri = "(01)09506000134352(17)261231(10)LOT(A)(21)SER(B)(710)PZN(C)"
		val expected = Gs1Medication(
			"09506000134352",
			"2026-12-31",
			"LOT(A)",
			"SER(B)",
			mapOf("710" to "PZN(C)")
		)

		assertEquals(expected, parse(plain))
		assertEquals(expected, parse(hri))
	}

	@Test
	fun acceptsSeparatorsAfterHumanReadableElements() {
		val hri = "(01)09506000134352\u001d(17)261231\u001d" +
			"(10)LOT(A)\u001d(21)SERIAL\u001d"

		assertEquals(
			Gs1Medication("09506000134352", "2026-12-31", "LOT(A)", "SERIAL"),
			parse(hri)
		)
	}

	@Test
	fun rejectsStrayTextAndSeparatorsInsideHumanReadableValues() {
		val hri = "(01)09506000134352(17)261231(10)LOT(21)SERIAL"

		assertNull(parse("STRAY" + hri))
		assertNull(parse(hri.replace("LOT", "LO\u001dT")))
	}

	private fun parse(
		text: String,
		format: String = "DataMatrix",
		currentYear: Int = 2026
	) = Gs1MedicationParser.parse(text, format, currentYear)
}
