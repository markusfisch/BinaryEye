package de.markusfisch.android.binaryeye.actions.vtype.vcard

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IntentAction
import de.markusfisch.android.binaryeye.actions.vtype.VTypeParser
import java.util.Locale

object VCardAction : IntentAction() {
	override val iconResId: Int
		get() = R.drawable.ic_action_vcard
	override val titleResId: Int
		get() = R.string.vcard_add
	override val errorMsg: Int
		get() = R.string.vcard_failed

	override fun canExecuteOn(data: ByteArray): Boolean {
		var stringData = String(data)
		// Quick & dirty MECARD support: simply make it a VCARD.
		if (stringData.startsWith("MECARD:")) {
			stringData = stringData
				.trim()
				.replace("MECARD:", "BEGIN:VCARD\n")
				.replace(";;", "\nEND:VCARD\n")
				.replace(";", "\n")
		}
		return VTypeParser.parseVType(stringData) == "VCARD"
	}

	override suspend fun createIntent(context: Context, data: ByteArray): Intent {
		val info = VTypeParser.parseMap(String(data))

		return Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
			type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
			info["FN"]?.singleOrNull()?.also { name ->
				putExtra(ContactsContract.Intents.Insert.NAME, name.value)
			} ?: info["N"]?.singleOrNull()?.also { name ->
				putExtra(ContactsContract.Intents.Insert.NAME, name.value.nameFormat)
			}
			info["TEL"]?.filter { it.value.isNotEmpty() }?.forEachIndexed { index, phoneProperty ->
				val (extraPhoneType, extraPhone) = when (index) {
					0 -> ContactsContract.Intents.Insert.PHONE_TYPE to
							ContactsContract.Intents.Insert.PHONE

					1 -> ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE to
							ContactsContract.Intents.Insert.SECONDARY_PHONE

					2 -> ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE to
							ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE

					else -> return@forEachIndexed
				}
				phoneProperty.firstTypeOrFirstInfo?.also {
					it.phoneType?.also { phoneType ->
						putExtra(extraPhoneType, phoneType)
					} ?: putExtra(extraPhoneType, it)
				}
				putExtra(
					extraPhone,
					phoneProperty.value.lowercase(Locale.US).removePrefix("tel:")
				)
			}
			info["EMAIL"]?.forEachIndexed { index, mailProperty ->
				val (extraMailType, extraMail) = when (index) {
					0 -> ContactsContract.Intents.Insert.EMAIL_TYPE to
							ContactsContract.Intents.Insert.EMAIL

					1 -> ContactsContract.Intents.Insert.SECONDARY_EMAIL_TYPE to
							ContactsContract.Intents.Insert.SECONDARY_EMAIL

					2 -> ContactsContract.Intents.Insert.TERTIARY_EMAIL_TYPE to
							ContactsContract.Intents.Insert.TERTIARY_EMAIL

					else -> return@forEachIndexed
				}
				mailProperty.firstTypeOrFirstInfo?.also {
					it.mailType?.also { mailType ->
						putExtra(extraMailType, mailType)
					} ?: putExtra(extraMailType, it)
				}
				putExtra(extraMail, mailProperty.value)
			}
			info["ORG"]?.singleOrNull()?.also { organization ->
				putExtra(ContactsContract.Intents.Insert.COMPANY, organization.value)
			}
			info["TITLE"]?.singleOrNull()?.also { jobTitle ->
				putExtra(ContactsContract.Intents.Insert.JOB_TITLE, jobTitle.value)
			}
			info["ADR"]?.forEach { addr ->
				addr.firstTypeOrFirstInfo?.also {
					it.addressType?.also { addressType ->
						putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE, addressType)
					} ?: putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE, it)
				}
				putExtra(ContactsContract.Intents.Insert.POSTAL, addr.value.locationFormat)
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				val urls = info["URL"]?.mapNotNull {
					it.value.takeIf { url -> url.isNotBlank() }?.let { url ->
						ContentValues(2).apply {
							put(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE
							)
							put(ContactsContract.CommonDataKinds.Website.URL, url)
						}
					}
				}
				val bDays = info["BDAY"]?.mapNotNull {
					it.value.takeIf { day -> day.isNotBlank() }?.let { day ->
						ContentValues(3).apply {
							put(
								ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
							)
							put(
								ContactsContract.CommonDataKinds.Event.TYPE,
								ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
							)
							put(ContactsContract.CommonDataKinds.Event.START_DATE, day)
						}
					}
				}
				putParcelableArrayListExtra(
					ContactsContract.Intents.Insert.DATA,
					ArrayList(listOfNotNull(urls, bDays).flatten())
				)
			}
		}
	}

	private val String.locationFormat: String
		get() = """^([^;]*?);([^;]*?);([^;]*?);([^;]*?);([^;]*?);([^;]*?);([^;]*?)$""".toRegex()
			.matchEntire(
				this
			)?.groupValues?.let {
				listOf(
					it[1],
					it[2],
					it[3],
					"${it[4]} ${it[6]}",
					"${it[5]} ${it[7]}"
				).filter { line -> line.isNotBlank() }
					.joinToString("\n").takeIf { location -> location.isNotBlank() }
			} ?: this

	private val String.nameFormat: String
		get() = """^([^;]*?);([^;]*?);([^;]*?);([^;]*?);([^;]*?)$""".toRegex()
			.matchEntire(this)?.groupValues?.let {
				listOf(it[4], it[2], it[3], it[1], it[5]).joinToString(" ")
					.takeIf { name -> name.isNotBlank() }
			} ?: this

	private val String.mailType: Int?
		get() = when (this.uppercase(Locale.US)) {
			"HOME" -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
			"WORK" -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
			"OTHER" -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
			"MOBILE" -> ContactsContract.CommonDataKinds.Email.TYPE_MOBILE
			else -> null
		}

	private val String.phoneType: Int?
		get() = when (this.uppercase(Locale.US)) {
			"HOME" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
			"MOBILE", "CELL" -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
			"WORK" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
			"FAX_WORK" -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK
			"FAX_HOME" -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME
			"PAGER" -> ContactsContract.CommonDataKinds.Phone.TYPE_PAGER
			"OTHER" -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
			"CALLBACK" -> ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK
			"CAR" -> ContactsContract.CommonDataKinds.Phone.TYPE_CAR
			"COMPANY_MAIN" -> ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN
			"ISDN" -> ContactsContract.CommonDataKinds.Phone.TYPE_ISDN
			"MAIN" -> ContactsContract.CommonDataKinds.Phone.TYPE_MAIN
			"OTHER_FAX" -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX
			"RADIO" -> ContactsContract.CommonDataKinds.Phone.TYPE_RADIO
			"TELEX" -> ContactsContract.CommonDataKinds.Phone.TYPE_TELEX
			"TTY_TDD" -> ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD
			"WORK_MOBILE" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE
			"WORK_PAGER" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER
			"ASSISTANT" -> ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT
			"MMS" -> ContactsContract.CommonDataKinds.Phone.TYPE_MMS
			else -> null
		}

	private val String.addressType: Int?
		get() = when (this.uppercase()) {
			"HOME" -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
			"WORK" -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
			"OTHER" -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
			else -> null
		}
}
