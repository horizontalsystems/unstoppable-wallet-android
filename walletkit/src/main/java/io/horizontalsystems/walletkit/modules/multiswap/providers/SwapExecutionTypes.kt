package io.horizontalsystems.walletkit.modules.multiswap.providers

/**
 * `executionType` per server provider id, as published by `/v3/providers`. Route capabilities
 * that depend on how a provider settles are read from here, never from a provider id, so the
 * server decides per provider. Stored as JSON beside the suspensions and restored on launch.
 */
data class SwapExecutionTypes(
    val byProvider: Map<String, String> = mapOf(),
) {
    /**
     * Whether a provider can pay a ZEC output to a shielded / unified receiver. Vault-settled
     * (thorchain_deposit) providers deliver from their own Zcash client, which addresses unified
     * receivers; transfer venues reject shielded recipients at order creation.
     */
    fun deliversShieldedZcash(providerId: String): Boolean =
        byProvider[SwapSuspensionIndex.serverId(providerId)] == ThorChainDepositRoute.METHOD

    companion object {
        fun from(responses: List<UnstoppableAPI.Response.Provider>) = SwapExecutionTypes(
            responses
                .filter { !it.executionType.isNullOrEmpty() }
                .associate { SwapSuspensionIndex.serverId(it.provider) to it.executionType!! },
        )
    }
}
