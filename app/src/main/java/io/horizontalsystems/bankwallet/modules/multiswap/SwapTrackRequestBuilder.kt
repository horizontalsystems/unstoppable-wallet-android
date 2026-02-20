package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.modules.multiswap.providers.MayaProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.OneInchProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.ThorChainProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.UProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.UnstoppableAPI

object SwapTrackRequestBuilder {

    // ChainId by blockchainTypeUid for EVM chains
    private val evmChainIds = mapOf(
        "ethereum" to "1",
        "binanceSmartChain" to "56",
        "polygon" to "137",
        "avalanche" to "43114",
        "optimism" to "10",
        "base" to "8453",
        "arbitrumOne" to "42161",
        "gnosis" to "100",
        "fantom" to "250",
    )

    fun build(record: SwapRecord): UnstoppableAPI.Request.Track {
        val providerApiName = apiProviderName(record.providerId)

        return when (record.providerId) {
            ThorChainProvider.id, MayaProvider.id -> UnstoppableAPI.Request.Track(
                provider = providerApiName,
                // Use hash when available; fall back to depositAddress for memoless swaps
                hash = record.transactionHash,
                depositAddress = if (record.transactionHash == null) record.depositAddress else null,
                fromAsset = record.fromAsset,
                toAsset = record.toAsset,
                toAddress = record.recipientAddress,
            )

            OneInchProvider.id -> UnstoppableAPI.Request.Track(
                provider = providerApiName,
                hash = record.transactionHash,
                chainId = evmChainIds[record.tokenInBlockchainTypeUid],
                fromAsset = record.fromAsset,
                toAsset = record.toAsset,
                toAddress = record.recipientAddress,
            )

            "u_${UProvider.Near.id}" -> UnstoppableAPI.Request.Track(
                provider = providerApiName,
                depositAddress = record.depositAddress,
                fromAddress = record.sourceAddress,
            )

            "u_${UProvider.QuickEx.id}",
            "u_${UProvider.LetsExchange.id}",
            "u_${UProvider.StealthEx.id}",
            "u_${UProvider.Swapuz.id}" -> UnstoppableAPI.Request.Track(
                provider = providerApiName,
                providerSwapId = record.providerSwapId,
                fromAddress = record.sourceAddress,
            )

            else -> throw IllegalArgumentException("Unsupported provider for tracking: ${record.providerId}")
        }
    }

    private fun apiProviderName(providerId: String): String = when (providerId) {
        ThorChainProvider.id -> "THORCHAIN"
        MayaProvider.id -> "MAYACHAIN"
        OneInchProvider.id -> "ONEINCH"
        else -> if (providerId.startsWith("u_")) providerId.removePrefix("u_") else providerId.uppercase()
    }
}
