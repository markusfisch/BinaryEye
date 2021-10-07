package de.markusfisch.android.binaryeye.link_utils

import java.util.regex.Pattern

private val urlPattern: Pattern = Pattern.compile(
    "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)" +
            "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*" +
            "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
    Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL
)

fun getLinks(text: String): List<String> {
    val matcher = urlPattern.matcher(text)

    val urls = mutableListOf<String>()

    while (matcher.find()) {
        val matchStart = matcher.start(1)
        val matchEnd = matcher.end()

        var url = text.substring(matchStart, matchEnd)
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            url = "https://$url"

        urls.add(url)
    }

    return urls
}