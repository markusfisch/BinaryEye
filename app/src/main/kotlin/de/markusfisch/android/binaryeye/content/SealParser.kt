package de.markusfisch.android.binaryeye.content

import android.content.Context
import de.markusfisch.android.binaryeye.R
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.idb.IdbSeal
import org.json.JSONArray
import org.json.JSONObject
import de.tsenger.vdstools.generic.SealParser as VdsSealParser

data class SealField(val name: Any, val value: String)
private data class IdbChildMessageDefinition(
	val name: String,
	val coding: MessageCoding
)

object SealParser {
	private val idbChildMessagesByName: Map<String, Map<Int, IdbChildMessageDefinition>> by lazy {
		loadIdbChildMessagesByName()
	}

	fun parse(
		context: Context,
		bytes: ByteArray
	): List<SealField>? = try {
		context.buildFields(
			VdsSealParser().parse(bytes.toString(Charsets.ISO_8859_1))
		)
	} catch (_: Exception) {
		null
	}

	private fun Context.buildFields(seal: Seal): List<SealField> = buildList {
		add(SealField(R.string.vds_document_type, seal.documentType))
		add(SealField(R.string.vds_issuing_country, seal.issuingCountry))
		seal.signatureInfo?.signingDate?.let {
			add(SealField(R.string.vds_signing_date, it.toString()))
		}
		if (seal is IdbSeal) {
			addAll(IdbVerifier.verify(this@buildFields, seal))
		}
		for (message in seal.messageList) {
			addAll(message.toFields())
		}
	}

	private fun Message.toFields(): List<SealField> = buildList {
		add(
			if (value is MessageValue.BytesValue) {
				SealField(
					"$name (${value.rawBytes.size} bytes)",
					value.rawBytes.toHexString().uppercase()
				)
			} else {
				SealField(name, value.toString())
			}
		)
		for (child in childMessages()) {
			addAll(child.toFields())
		}
	}

	private fun Message.childMessages(): List<Message> {
		val childDefinitionsByTag = idbChildMessagesByName[name] ?: return emptyList()
		return runCatching {
			DerTlv.parseAll(value.rawBytes).mapNotNull { derTlv ->
				val childTag = derTlv.tag.toInt() and 0xff
				val childDefinition = childDefinitionsByTag[childTag]
					?: return@mapNotNull null
				Message(
					childTag,
					childDefinition.name,
					childDefinition.coding,
					MessageValue.fromBytes(
						derTlv.value,
						childDefinition.coding
					)
				)
			}
		}.getOrElse {
			emptyList()
		}
	}

	private fun loadIdbChildMessagesByName(): Map<String, Map<Int, IdbChildMessageDefinition>> {
		val resourceConstantsClass = runCatching {
			Class.forName("de.tsenger.vdstools.generated.ResourceConstants")
		}.getOrElse {
			return emptyMap()
		}
		val json = runCatching {
			resourceConstantsClass.getField(
				"IDB_MESSAGE_TYPES_JSON"
			).get(null) as String
		}.getOrElse {
			return emptyMap()
		}
		val childMessagesByName =
			mutableMapOf<String, Map<Int, IdbChildMessageDefinition>>()
		val messageTypes = JSONArray(json)
		for (i in 0 until messageTypes.length()) {
			val messageType = messageTypes.getJSONObject(i)
			val messages = messageType.optJSONArray("messages") ?: continue
			collectChildMessages(
				messageType.getString("name"),
				messages,
				childMessagesByName
			)
		}
		return childMessagesByName
	}

	private fun collectChildMessages(
		parentName: String,
		messages: JSONArray,
		childMessagesByName: MutableMap<String, Map<Int, IdbChildMessageDefinition>>
	) {
		val childMessages = mutableMapOf<Int, IdbChildMessageDefinition>()
		for (i in 0 until messages.length()) {
			val message = messages.getJSONObject(i)
			val childDefinition = message.toChildMessageDefinition()
			childMessages[message.getInt("tag")] = childDefinition
			val nestedMessages = message.optJSONArray("messages") ?: continue
			collectChildMessages(
				childDefinition.name,
				nestedMessages,
				childMessagesByName
			)
		}
		if (childMessages.isNotEmpty()) {
			childMessagesByName[parentName] = childMessages
		}
	}

	private fun JSONObject.toChildMessageDefinition(): IdbChildMessageDefinition {
		return IdbChildMessageDefinition(
			getString("name"),
			MessageCoding.valueOf(getString("coding"))
		)
	}
}
