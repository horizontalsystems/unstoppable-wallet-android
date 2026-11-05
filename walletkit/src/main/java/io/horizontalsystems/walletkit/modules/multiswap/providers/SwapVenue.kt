package io.horizontalsystems.walletkit.modules.multiswap.providers

/**
 * A server provider as the app knows it through the synced `GET /v3/providers` rows: the venue a
 * route is served by. Nothing about a venue is hardcoded here; a provider the server adds shows
 * up as soon as the list is synced. [isKnown] is false for an id the last sync did not list: a
 * humanized name and the route's own `amlPolicy` stand in until the next sync.
 */
data class SwapVenue(
    val id: String,
    val name: String,
    // The server's `amlPolicy`, the Route Score shown beside a quote; null when it has not rated the provider.
    val riskLevel: RiskLevel?,
    // `none` | `basic` | ...; any value other than `none` is a confidential rail.
    val privacy: String,
    val executionType: String?,
    val isKnown: Boolean,
) {
    val isConfidential: Boolean
        get() = privacy != NO_PRIVACY

    /** The route's own `amlPolicy` wins over the synced row's when present: it is the policy of this quote. */
    fun overriding(amlPolicy: String?): SwapVenue {
        val level = amlPolicy?.let { RiskLevel.fromAmlPolicy(it) } ?: return this
        return copy(riskLevel = level)
    }

    companion object {
        const val NO_PRIVACY = "none"

        /** `PANCAKESWAP_API` -> `Pancakeswap Api`: the name for an id the provider list has none for. */
        fun humanized(id: String): String = id
            .split('_')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word -> word.take(1).uppercase() + word.drop(1).lowercase() }

        // Keyed like a synced row, so a record made before the sync resolves the same way after it.
        fun unknown(id: String) = SwapVenue(
            id = id.uppercase(),
            name = humanized(id),
            riskLevel = null,
            privacy = NO_PRIVACY,
            executionType = null,
            isKnown = false,
        )
    }
}
