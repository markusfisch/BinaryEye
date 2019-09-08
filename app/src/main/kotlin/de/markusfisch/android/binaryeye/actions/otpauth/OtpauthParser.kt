package de.markusfisch.android.binaryeye.actions.otpauth

import android.net.Uri

class OtpauthParser private constructor(private val match: MatchResult) {
	val otpType: String
		get() = match.groupValues[1]
	private val labelIssuer: String
		get() = match.groupValues[2]
	val accountName: String
		get() = match.groupValues[3].takeIf { it.isNotEmpty() } ?: match.groupValues[4]
	val secret: String
		get() = match.groupValues[5]
	private val parameterIssuer: String
		get() = match.groupValues[6]
	val algorithm: String
		get() = match.groupValues[7].takeIf { it.isNotEmpty() } ?: "SHA1"
	val digits: Int
		get() = match.groupValues[8].toIntOrNull() ?: 6
	val period: Int
		get() = match.groupValues[9].toIntOrNull() ?: 30
	private val parameterCounter: String
		get() = match.groupValues[10]
	val issuer: String
		get() = labelIssuer.takeIf { it.isNotEmpty() } ?: parameterIssuer
	val counter: Int
		get() = parameterCounter.toInt()
	private val isHotp: Boolean = otpType == "hotp"
	val isValid: Boolean =
		(labelIssuer.isNotEmpty() == parameterIssuer.isEmpty() || labelIssuer.isEmpty())
				&& secret.isNotEmpty() && isHotp == parameterCounter.isNotEmpty()

	val uri: Uri
		get() = Uri.Builder().apply {
			scheme("otpauth")
			authority(otpType)

			if (issuer.isNotEmpty()) {
				encodedPath("/$issuer:$accountName")
				appendQueryParameter("issuer", issuer)
			} else {
				encodedPath("/$accountName")
			}

			appendQueryParameter("secret", secret)
			appendQueryParameter("algorithm", algorithm)
			appendQueryParameter("digits", digits.toString())

			if (isHotp) {
				appendQueryParameter("counter", counter.toString())
			} else {
				appendQueryParameter("period", period.toString())
			}
		}.build()

	companion object {
		private const val issuerAndAccount =
			"""(?:(?:([^?:]+)(?::|%3A)(?:%20)*([^?]+))|([^?]+))"""//.toRegex()
		/** allowing `padding` even though it should not be there, will be removed in output*/
		private const val secret = """(?:secret=([2-7A-Z]+)=*)"""//.toRegex()
		/** `\2` references the issuer of [issuerAndAccount] */
		private const val issuer = """(?:issuer=\2)|(?:issuer=([^&]+))"""//.toRegex()
		private const val algorithm = """(?:algorithm=(SHA(?:1|256|512)))"""//.toRegex()
		private const val digits = """(?:digits=([0-9]+))"""//.toRegex()
		private const val period = """(?:period=([0-9]+))"""//.toRegex()
		private const val counter = """(?:counter=([0-9]+))"""//.toRegex()
		private val otpauthRegex =
			"""^otpauth://([ht]otp)/$issuerAndAccount\?(?:&?(?:$secret|$issuer|$algorithm|$digits|$period|$counter))+$""".toRegex(
				RegexOption.IGNORE_CASE
			)

		operator fun invoke(input: String): OtpauthParser? {
			return otpauthRegex.matchEntire(input)?.let(::OtpauthParser)?.takeIf { it.isValid }
		}
	}
}
