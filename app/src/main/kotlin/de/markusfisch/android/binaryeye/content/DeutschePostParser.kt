package de.markusfisch.android.binaryeye.content

import de.markusfisch.android.zxingcpp.ZxingCpp

data class DeutschePostTrackingLink(
	val label: String,
	val link: String
)

object DeutschePostParser {
	fun parse(
		bytes: ByteArray,
		format: String
	): DeutschePostTrackingLink? {
		var isStamp = false
		var rawData = bytes
		if (format == ZxingCpp.BarcodeFormat.DataMatrix.name &&
			bytes.toString(Charsets.ISO_8859_1).startsWith("DEA5")
		) {
			if (bytes.size == 47) {
				isStamp = true
			} else if (bytes.size > 47) {
				// Transform back to original data.
				rawData = bytes.toString(Charsets.UTF_8).toByteArray(
					Charsets.ISO_8859_1
				)
				if (rawData.size == 47) {
					isStamp = true
				}
			}
		}

		if (!isStamp) {
			return null
		}

		val hex = StringBuilder()
		hex.append(String.format("%02X", rawData[9]))
		hex.append(String.format("%02X", rawData[10]))
		hex.append(String.format("%02X", rawData[11]))
		hex.append(String.format("%02X", rawData[12]))
		hex.append(String.format("%02X", rawData[13]))
		hex.append(String.format("%X", (rawData[4].toInt() and 0x0f).toByte()))
		hex.append(String.format("%02X", rawData[5]))
		hex.append(String.format("%02X", rawData[6]))
		hex.append(String.format("%02X", rawData[7]))
		hex.append(String.format("%02X", rawData[8]))
		val hexString = hex.toString()
		val trackingNumber = hexString + String.format(
			"%X",
			crc4(hexString.toByteArray(Charsets.ISO_8859_1))
		)
		return DeutschePostTrackingLink(
			trackingNumber,
			"https://www.deutschepost.de/de/s/sendungsverfolgung.html?piececode=$trackingNumber",
		)
	}

	// CRC-4 with polynomial x^4 + x + 1.
	private fun crc4(input: ByteArray): Int {
		var crc = 0
		var i = 0
		while (i < input.size) {
			val c = input[i].toInt()
			var j = 0x80
			while (j != 0) {
				var bit = crc and 0x8
				crc = crc shl 1
				if (c and j != 0) {
					bit = bit xor 0x8
				}
				if (bit != 0) {
					crc = crc xor 0x3
				}
				j = j ushr 1
			}
			++i
		}
		crc = crc and 0xF
		return crc
	}
}