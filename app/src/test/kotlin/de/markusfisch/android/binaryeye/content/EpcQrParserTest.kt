package de.markusfisch.android.binaryeye.content

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class EpcQrParserTest {
	@Test
	fun parsesRequiredFields() {
		val info = EpcQrParser.parse(
			"BCD\n002\n1\nSCT\n\nWikimedia Foerdergesellschaft\nDE12345678901234567890"
		) ?: throw AssertionError("expected valid EPC QR content")

		assertEquals("BCD", info[EpcQrElement.SERVICE_TAG])
		assertEquals("002", info[EpcQrElement.VERSION])
		assertEquals("UTF-8", info[EpcQrElement.CHARSET])
		assertEquals("SCT", info[EpcQrElement.IDENTIFICATION])
		assertEquals("Wikimedia Foerdergesellschaft", info[EpcQrElement.NAME])
		assertEquals("DE12345678901234567890", info[EpcQrElement.IBAN])
	}

	@Test
	fun parsesOptionalFieldsWithCrLf() {
		val info = EpcQrParser.parse(
			"BCD\r\n001\r\n1\r\nSCT\r\nBFSWDE33BER\r\nWikimedia Foerdergesellschaft\r\nDE12345678901234567890\r\nEUR123.45\r\nBENE\r\nRF18539007547034\r\n\r\nInvoice 2026"
		) ?: throw AssertionError("expected valid EPC QR content")

		assertEquals("BFSWDE33BER", info[EpcQrElement.BIC])
		assertEquals("123.45", info[EpcQrElement.AMOUNT])
		assertEquals("BENE", info[EpcQrElement.PURPOSE])
		assertEquals("RF18539007547034", info[EpcQrElement.REFERENCE])
		assertEquals("Invoice 2026", info[EpcQrElement.INFORMATION])
	}

	@Test
	fun rejectsInvalidHeader() {
		assertNull(
			EpcQrParser.parse(
				"XXX\n002\n1\nSCT\n\nRecipient\nDE12345678901234567890"
			)
		)
	}

	@Test
	fun rejectsMixedReferenceAndText() {
		assertNull(
			EpcQrParser.parse(
				"BCD\n002\n1\nSCT\n\nRecipient\nDE12345678901234567890\nEUR1.00\n\nRF18539007547034\nMemo"
			)
		)
	}
}
