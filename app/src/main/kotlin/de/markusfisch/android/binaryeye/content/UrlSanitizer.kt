package de.markusfisch.android.binaryeye.content

import android.net.Uri

object UrlSanitizer {

	private val TRACKING_PARAMS = setOf(
		"utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
		"utm_id", "utm_name", "utm_referrer", "utm_brand", "utm_social",
		"fbclid", "gclid", "dclid", "gclsrc", "yclid", "msclkid",
		"mc_eid", "mc_cid",
		"_ga", "_gl", "ga_source", "ga_medium",
		"igshid", "igsh",
		"hsa_acc", "hsa_cam", "hsa_grp", "hsa_ad", "hsa_src",
		"hsa_tgt", "hsa_kw", "hsa_mt", "hsa_net", "hsa_ver",
		"spm", "scm", "vero_id", "vero_conv"
	)

	fun stripTrackingParams(uri: Uri): Uri {
		val scheme = uri.scheme?.lowercase()
		if (scheme != "http" && scheme != "https") return uri
		val queryNames = runCatching {
			uri.queryParameterNames
		}.getOrDefault(emptySet())
		if (queryNames.isEmpty()) return uri
		val keep = queryNames.filter { paramName ->
			paramName.lowercase() !in TRACKING_PARAMS
		}
		if (keep.size == queryNames.size) return uri
		val builder = uri.buildUpon().clearQuery()
		for (paramName in keep) {
			uri.getQueryParameters(paramName).forEach { paramValue ->
				builder.appendQueryParameter(paramName, paramValue)
			}
		}
		return builder.build()
	}
}
