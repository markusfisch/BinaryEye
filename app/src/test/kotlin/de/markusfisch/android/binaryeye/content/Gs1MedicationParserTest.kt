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

	private fun parse(
		text: String,
		format: String = "DataMatrix",
		currentYear: Int = 2026
	) = Gs1MedicationParser.parse(text, format, currentYear)
}
