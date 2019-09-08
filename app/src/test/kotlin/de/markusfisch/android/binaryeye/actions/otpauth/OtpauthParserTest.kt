package de.markusfisch.android.binaryeye.actions.otpauth

import de.markusfisch.android.binaryeye.assertThrows
import de.markusfisch.android.binaryeye.simpleFail
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class OtpauthParserTest {
	@Test
	fun noOtpauthUri() {
		assertNull(OtpauthParser("random string"))
	}

	@Test
	fun missingSecret() {
		assertNull(OtpauthParser("otpauth://totp/issuerName:accountName"))
	}

	@Test
	fun missingCounterHotp() {
		assertNull(OtpauthParser("otpauth://hotp/accountName?secret=secretString"))
	}

	@Test
	fun minimalHotp() {
		val parser = validParser("otpauth://hotp/accountName?secret=secretString&counter=50")
		assertEquals("hotp", parser.otpType)
		assertEquals("accountName", parser.accountName)
		assertEquals("secretString", parser.secret)
		assertEquals("SHA1", parser.algorithm)
		assertEquals(6, parser.digits)
		assertEquals(30, parser.period)
		assertEquals(50, parser.counter)
	}

	@Test
	fun normalHotp() {
		val parser = validParser("otpauth://hotp/issuerName:accountName?secret=secretString&counter=0")
		assertEquals("hotp", parser.otpType)
		assertEquals("issuerName", parser.issuer)
		assertEquals("accountName", parser.accountName)
		assertEquals("secretString", parser.secret)
		assertEquals("SHA1", parser.algorithm)
		assertEquals(6, parser.digits)
		assertEquals(30, parser.period)
		assertEquals(0, parser.counter)

	}

	@Test
	fun missingIssuerParamHotp() {
		val parser = validParser("otpauth://hotp/issuerName:accountName?secret=secretString&counter=50&algorithm=SHA256&digits=8")
		assertEquals("hotp", parser.otpType)
		assertEquals("issuerName", parser.issuer)
		assertEquals("accountName", parser.accountName)
		assertEquals("secretString", parser.secret)
		assertEquals("SHA256", parser.algorithm)
		assertEquals(8, parser.digits)
		assertEquals(30, parser.period)
		assertEquals(50, parser.counter)
	}

	@Test
	fun fullHotp() {
		val parser = validParser("otpauth://hotp/issuerName:accountName?secret=secretString&counter=50&issuer=issuerName&algorithm=SHA256&digits=8")
		assertEquals("hotp", parser.otpType)
		assertEquals("issuerName", parser.issuer)
		assertEquals("accountName", parser.accountName)
		assertEquals("secretString", parser.secret)
		assertEquals("SHA256", parser.algorithm)
		assertEquals(8, parser.digits)
		assertEquals(30, parser.period)
		assertEquals(50, parser.counter)
	}

	@Test
	fun minimalTotp() {
		val parser = validParser("otpauth://totp/accountName?secret=secretString")
		assertEquals("totp", parser.otpType)
		assertEquals("accountName", parser.accountName)
		assertEquals("secretString", parser.secret)
		assertEquals("SHA1", parser.algorithm)
		assertEquals(6, parser.digits)
		assertEquals(30, parser.period)
		assertThrows<NumberFormatException> {
			parser.counter
		}
	}

	@Test
	fun normalTotp() {
		val parser = validParser("otpauth://totp/issuerName:accountName?secret=secretString")
		assertEquals("totp", parser.otpType)
		assertEquals("issuerName", parser.issuer)
		assertEquals("accountName", parser.accountName)
		assertEquals("secretString", parser.secret)
		assertEquals("SHA1", parser.algorithm)
		assertEquals(6, parser.digits)
		assertEquals(30, parser.period)
		assertThrows<NumberFormatException> {
			parser.counter
		}
	}

	@Test
	fun missingIssuerParamTotp() {
		val parser = validParser("otpauth://totp/issuerName:accountName?secret=secretString&algorithm=SHA256&digits=8&period=60")
		assertEquals("totp", parser.otpType)
		assertEquals("issuerName", parser.issuer)
		assertEquals("accountName", parser.accountName)
		assertEquals("secretString", parser.secret)
		assertEquals("SHA256", parser.algorithm)
		assertEquals(8, parser.digits)
		assertEquals(60, parser.period)
		assertThrows<NumberFormatException> {
			parser.counter
		}
	}

	@Test
	fun fullTotp() {
		val parser = validParser("otpauth://totp/issuerName:accountName?secret=secretString&issuer=issuerName&algorithm=SHA256&digits=8&period=60")
		assertEquals("totp", parser.otpType)
		assertEquals("issuerName", parser.issuer)
		assertEquals("accountName", parser.accountName)
		assertEquals("secretString", parser.secret)
		assertEquals("SHA256", parser.algorithm)
		assertEquals(8, parser.digits)
		assertEquals(60, parser.period)
		assertThrows<NumberFormatException> {
			parser.counter
		}
	}

	private fun validParser(input: String): OtpauthParser {
		return OtpauthParser(input) ?: simpleFail("Could not get parser of valid input")
	}
}
