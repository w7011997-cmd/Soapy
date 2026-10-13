package com.soapy.app.data

/** What the user typed, cleaned up. [value] is what gets stored and matched. */
data class ParsedTerm(val value: String, val isSite: Boolean)

object TermParser {

    private val HOST = Regex("^([a-z0-9-]+\\.)+[a-z]{2,}$")

    /**
     * Turns raw input into a word or a website host.
     * "https://www.Example.com/page?x=1" becomes the site "example.com".
     * Returns null when the input is too short or is a broken web address.
     */
    fun parse(raw: String): ParsedTerm? {
        var text = raw.trim().lowercase()
        if (text.isEmpty()) return null

        val hasScheme = text.startsWith("http://") || text.startsWith("https://")
        if (hasScheme) text = text.substringAfter("://")

        val host = text
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
            .removePrefix("www.")

        if (hasScheme || text.contains('/')) {
            return if (HOST.matches(host)) ParsedTerm(host, true) else null
        }
        if (!text.contains(' ') && HOST.matches(host)) {
            return ParsedTerm(host, true)
        }
        return if (text.length >= 2) ParsedTerm(text, false) else null
    }
}
