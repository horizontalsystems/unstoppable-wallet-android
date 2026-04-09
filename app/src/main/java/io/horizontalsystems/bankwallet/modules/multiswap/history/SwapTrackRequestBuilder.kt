package io.horizontalsystems.bankwallet.modules.multiswap.history

import io.horizontalsystems.bankwallet.entities.SwapRecord
import io.horizontalsystems.bankwallet.modules.multiswap.providers.MayaProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.MultiSwapProviderRegistry
import io.horizontalsystems.bankwallet.modules.multiswap.providers.OneInchProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.PancakeSwapV3Provider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.ThorChainProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.UProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.UniswapV3Provider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.UnstoppableAPI

object SwapTrackRequestBuilder {

    // ChainId by blockchainTypeUid for EVM chains
    private val evmChainIds = mapOf(
        "ethereum" to "1",
        "binance-smart-chain" to "56",
        "polygon-pos" to "137",
        "avalanche" to "43114",
        "optimistic-ethereum" to "10",
        "base" to "8453",
        "arbitrum-one" to "42161",
        "gnosis" to "100",
    )

    fun build(record: SwapRecord): UnstoppableAPI.Request.Track {
        val providerApiName = apiProviderName(record.providerId)

        return when {
            record.providerId == ThorChainProvider.id || record.providerId == MayaProvider.id -> UnstoppableAPI.Request.Track(
                provider = providerApiName,
                // Use hash when available; fall back to depositAddress for memoless swaps
                hash = record.transactionHash,
                depositAddress = if (record.transactionHash == null) record.depositAddress else null,
                fromAsset = record.fromAsset,
                toAsset = record.toAsset,
                toAddress = record.recipientAddress,
            )

            MultiSwapProviderRegistry.isEvm(record.providerId) -> UnstoppableAPI.Request.Track(
                provider = providerApiName,
                hash = record.transactionHash,
                chainId = evmChainIds[record.tokenInBlockchainTypeUid],
                fromAsset = record.fromAsset,
                toAsset = record.toAsset,
                toAddress = record.recipientAddress,
            )

            record.providerId == "u_${UProvider.Near.id}" -> UnstoppableAPI.Request.Track(
                provider = providerApiName,
                depositAddress = record.depositAddress,
                fromAddress = record.sourceAddress,
            )

            record.providerId == "u_${UProvider.QuickEx.id}" ||
            record.providerId == "u_${UProvider.LetsExchange.id}" ||
            record.providerId == "u_${UProvider.StealthEx.id}" ||
            record.providerId == "u_${UProvider.Exolix.id}" ||
            record.providerId == "u_${UProvider.Swapuz.id}" -> UnstoppableAPI.Request.Track(
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
        PancakeSwapV3Provider.id -> "PANCAKESWAP"
        UniswapV3Provider.id -> "UNISWAP_V3"
        else -> if (providerId.startsWith("u_")) providerId.removePrefix("u_") else providerId.uppercase()
    }
}
