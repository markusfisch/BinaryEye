package de.markusfisch.android.binaryeye.content

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.Action
import de.markusfisch.android.binaryeye.actions.ActionRegistry
import de.markusfisch.android.binaryeye.actions.mail.MatMsg
import de.markusfisch.android.binaryeye.actions.mail.MatMsgAction
import de.markusfisch.android.binaryeye.actions.otpauth.OtpauthAction
import de.markusfisch.android.binaryeye.actions.vtype.VTypeParser
import de.markusfisch.android.binaryeye.actions.vtype.vcard.VCardAction
import de.markusfisch.android.binaryeye.actions.vtype.vevent.VEventAction
import de.markusfisch.android.binaryeye.actions.web.WebAction
import de.markusfisch.android.binaryeye.actions.wifi.WifiAction
import de.markusfisch.android.binaryeye.actions.wifi.WifiConnector
import org.json.JSONException
import org.json.JSONObject

enum class ParsedContentType(
	val resId: Int
) {
	UNKNOWN(0),
	DEUTSCHE_POST(R.string.parsed_type_deutsche_post),
	JSON(R.string.parsed_type_json),
	INTERNATIONAL_DRIVER_LICENSE(
		R.string.parsed_type_international_driver_license
	),
	DIGITAL_SEAL(R.string.parsed_type_digital_seal),
	SEPA_EPC_QR(R.string.parsed_type_sepa_epc_qr),
	EMAIL(R.string.parsed_type_email),
	URL(R.string.parsed_type_url),
	OTP(R.string.parsed_type_otp),
	CONTACT_CARD(R.string.parsed_type_contact_card),
	CALENDAR_EVENT(R.string.parsed_type_calendar_event),
	WIFI_NETWORK(R.string.parsed_type_wifi_network);
}

data class ParsedData(
	val type: ParsedContentType,
	val fields: List<ParsedField>
)

data class ParsedField(
	val key: Any,
	val value: CharSequence?,
	val link: String? = null
)

fun parseData(
	context: Context,
	text: String,
	bytes: ByteArray,
	format: String,
	action: Action = ActionRegistry.getAction(bytes)
): ParsedData {
	val fields = mutableListOf<ParsedField>()

	DeutschePostParser.parse(bytes, format)?.let {
		fields.addField(
			context.getString(R.string.tracking_number),
			it.label,
			it.link,
		)
		return ParsedData(ParsedContentType.DEUTSCHE_POST, fields)
	}

	try {
		fields.addField(R.string.formatted_json, JSONObject(text).toString(2))
		return ParsedData(ParsedContentType.JSON, fields)
	} catch (_: JSONException) {
		// Ignore
	}

	IdlParser.parse(String(bytes))?.let {
		fields.addField("IIN", it.iin)
		it.elements.forEach { (id, value) ->
			fields.addField(context.idlToRes(id), value)
		}
		return ParsedData(ParsedContentType.INTERNATIONAL_DRIVER_LICENSE, fields)
	}

	SealParser.parse(context, bytes)?.let { sealFields ->
		sealFields.forEach { vf ->
			fields.addField(vf.name, vf.value)
		}
		return ParsedData(ParsedContentType.DIGITAL_SEAL, fields)
	}

	EpcQrParser.parse(text)?.let {
		fields.addAll(it.map { (id, value) ->
			ParsedField(context.epcQrToRes(id), value)
		})
		return ParsedData(ParsedContentType.SEPA_EPC_QR, fields)
	}

	when (action) {
		is MatMsgAction -> MatMsg(text).run {
			fields.addField(R.string.email_to, to)
			fields.addField(R.string.email_subject, sub)
			fields.addField(R.string.email_body, body)
			return ParsedData(ParsedContentType.EMAIL, fields)
		}

		is VCardAction,
		is VEventAction -> VTypeParser.parseMap(text).let { parsedMap ->
			parsedMap.forEach { item ->
				fields.addField(item.key, item.value.joinToString("\n") { it.value })
			}
			return ParsedData(
				if (action is VCardAction) {
					ParsedContentType.CONTACT_CARD
				} else {
					ParsedContentType.CALENDAR_EVENT
				},
				fields
			)
		}

		is WebAction -> try {
			text.toUri().run {
				fields.addField(R.string.scheme, scheme)
				fields.addField(R.string.host, host)
				fields.addField(R.string.query, query)
			}
			return ParsedData(ParsedContentType.URL, fields)
		} catch (_: Exception) {
			// Ignore
		}

		is OtpauthAction -> try {
			text.toUri().run {
				val label = pathSegments.firstOrNull()?.let {
					Uri.decode(it)
				} ?: ""
				val colonIdx = label.indexOf(':')
				val issuerFromLabel = if (colonIdx >= 0) {
					label.substring(0, colonIdx)
				} else {
					null
				}
				val account = if (colonIdx >= 0) {
					label.substring(colonIdx + 1).trim()
				} else {
					label
				}
				val issuer = getQueryParameter("issuer") ?: issuerFromLabel
				fields.addField(R.string.entry_type, host?.uppercase())
				fields.addField(R.string.otp_account, account)
				fields.addField(R.string.otp_issuer, issuer)
				fields.addField(R.string.otp_algorithm, getQueryParameter("algorithm"))
				fields.addField(R.string.otp_digits, getQueryParameter("digits"))
				if (host == "totp") {
					fields.addField(R.string.otp_period, getQueryParameter("period"))
				} else {
					fields.addField(R.string.otp_counter, getQueryParameter("counter"))
				}
			}
			return ParsedData(ParsedContentType.OTP, fields)
		} catch (_: Exception) {
			// Ignore
		}

		is WifiAction -> WifiConnector.parseMap(text)?.let { wifiData ->
			fields.addField(R.string.wifi_ssid, wifiData["S"])
			fields.addField(R.string.wifi_password, wifiData["P"])
			fields.addField(R.string.wifi_type, wifiData["T"])
			fields.addField(R.string.wifi_hidden, wifiData["H"])
			fields.addField(R.string.wifi_eap, wifiData["E"])
			fields.addField(R.string.wifi_identity, wifiData["I"])
			fields.addField(R.string.wifi_anonymous_identity, wifiData["A"])
			fields.addField(R.string.wifi_phase2, wifiData["PH2"])
			return ParsedData(ParsedContentType.WIFI_NETWORK, fields)
		}
	}
	return ParsedData(ParsedContentType.UNKNOWN, emptyList())
}

fun parsedContentTypeFromName(
	name: String?
): ParsedContentType = try {
	ParsedContentType.valueOf(name ?: "")
} catch (_: IllegalArgumentException) {
	ParsedContentType.UNKNOWN
}

fun getContentPreview(
	context: Context,
	text: String,
	bytes: ByteArray,
	format: String,
	action: Action = ActionRegistry.getAction(bytes)
): CharSequence {
	val parsedData = parseData(context, text, bytes, format, action)
	return when (parsedData.type) {
		ParsedContentType.UNKNOWN -> text.ifEmpty {
			bytes.toHexString()
		}

		ParsedContentType.URL -> parsedData.valueFor(R.string.host)
			?: text

		ParsedContentType.WIFI_NETWORK -> parsedData.valueFor(R.string.wifi_ssid)
			?: parsedData.firstValue()
			?: text

		ParsedContentType.EMAIL -> parsedData.valueFor(R.string.email_to)
			?: parsedData.valueFor(R.string.email_subject)
			?: parsedData.firstValue()
			?: text

		ParsedContentType.OTP -> parsedData.valueFor(R.string.otp_account)
			?: parsedData.firstValue()
			?: text

		else -> parsedData.firstValue() ?: text
	}
}

private fun ParsedData.valueFor(key: Int): CharSequence? =
	fields.firstValue { it.key == key }

private fun ParsedData.firstValue(): CharSequence? =
	fields.firstValue()

private fun List<ParsedField>.firstValue(
	predicate: (ParsedField) -> Boolean = { true }
): CharSequence? = firstOrNull {
	predicate(it) && !it.value.isNullOrBlank()
}?.value

private fun MutableList<ParsedField>.addField(
	key: Any,
	value: CharSequence?,
	link: String? = null
) {
	add(ParsedField(key, value, link))
}
