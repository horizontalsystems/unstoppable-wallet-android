package io.horizontalsystems.bankwallet.modules.watchaddress

import java.net.URLDecoder

data class MoneroUriInfo(
    val address: String,
    val spendKey: String? = null,
    val viewKey: String? = null,
    val height: Long? = null
)

object MoneroUriParser {

    private val VALID_SCHEMES = listOf("monero_wallet", "monero")
    private const val SPEND_KEY_PARAM = "spend_key"
    private const val VIEW_KEY_PARAM = "view_key"
    private const val HEIGHT_PARAM = "height"

    /**
     * Parses a Monero wallet URI and extracts wallet information
     *
     * @param uriString The Monero wallet URI string
     * @return MoneroWalletInfo containing parsed data
     * @throws IllegalArgumentException if URI is invalid or not a Monero wallet URI
     */
    fun parse(uriString: String): MoneroUriInfo? {
        try {
            val colonIndex = uriString.indexOf(':')
            if (colonIndex == -1) {
                throw IllegalArgumentException("Invalid URI format: missing scheme separator ':'")
            }

            val scheme = uriString.substring(0, colonIndex)
            val schemeSpecificPart = uriString.substring(colonIndex + 1)

            // Validate scheme
            if (scheme !in VALID_SCHEMES) {
                throw IllegalArgumentException("Invalid scheme: expected one of $VALID_SCHEMES, got '$scheme'")
            }

            val queryIndex = schemeSpecificPart.indexOf('?')
            val address = if (queryIndex == -1) {
                schemeSpecificPart
            } else {
                schemeSpecificPart.substring(0, queryIndex)
            }

            if (address.isBlank()) {
                throw IllegalArgumentException("Address cannot be empty")
            }

            // Parse query parameters
            val query = if (queryIndex != -1 && queryIndex < schemeSpecificPart.length - 1) {
                schemeSpecificPart.substring(queryIndex + 1)
            } else {
                null
            }

            // Parse query parameters
            val queryParams = parseQueryParameters(query)

            val spendKey = queryParams[SPEND_KEY_PARAM]
            val viewKey = queryParams[VIEW_KEY_PARAM]
            val height = queryParams[HEIGHT_PARAM]?.toLongOrNull()

            return MoneroUriInfo(
                address = address,
                spendKey = spendKey,
                viewKey = viewKey,
                height = height
            )

        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Parses query parameters from a query string
     *
     * @param query The query string (without the '?' prefix)
     * @return Map of parameter names to values
     */
    private fun parseQueryParameters(query: String?): Map<String, String> {
        if (query.isNullOrBlank()) return emptyMap()

        return query.split('&')
            .mapNotNull { param ->
                val parts = param.split('=', limit = 2)
                if (parts.size == 2) {
                    val key = URLDecoder.decode(parts[0], "UTF-8")
                    val value = URLDecoder.decode(parts[1], "UTF-8")
                    key to value
                } else null
            }
            .toMap()
    }
}
