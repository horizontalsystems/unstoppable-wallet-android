package io.horizontalsystems.walletkit.modules.multiswap.providers

/**
 * `amlPolicy` per server provider id, as published by `/v3/providers`: the Route Score shown
 * beside a quote. The server owns the score (it is a database column there, changed without an
 * app release), so the app never carries one of its own. Stored as JSON beside the suspensions
 * and restored on launch.
 */
data class SwapRouteScores(
    val byProvider: Map<String, String> = mapOf(),
) {
    /** Null until the server has published a score for the provider, or for a value it does not know. */
    fun riskLevel(providerId: String): RiskLevel? =
        byProvider[SwapSuspensionIndex.serverId(providerId)]?.let { RiskLevel.fromAmlPolicy(it) }

    companion object {
        fun from(responses: List<UnstoppableAPI.Response.Provider>) = SwapRouteScores(
            responses
                .filter { !it.amlPolicy.isNullOrEmpty() }
                .associate { SwapSuspensionIndex.serverId(it.provider) to it.amlPolicy!! },
        )
    }
}
