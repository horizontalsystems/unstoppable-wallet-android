package io.horizontalsystems.bankwallet.modules.multiswap.history

import io.horizontalsystems.bankwallet.entities.SwapRecord
import io.horizontalsystems.bankwallet.modules.multiswap.providers.MayaProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.OneInchProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.PancakeSwapV3Provider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.ThorChainProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.UniswapV3Provider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.UnstoppableAPI

object SwapTrackRequestBuilder {

    // The /v2 track endpoint a record is tracked through.
    //   Recorded  — our USwap-mediated swaps; tracked by the record uuid alone.
    //   Evm       — native single-tx EVM swaps (1inch/Uniswap/Pancake); stateless reader.
    //   Thorchain — native THORChain/Mayachain swaps; stateless reader.
    enum class Endpoint { Recorded, Evm, Thorchain }

    data class TrackCall(val endpoint: Endpoint, val request: UnstoppableAPI.Request.Track)

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

    fun build(record: SwapRecord): TrackCall {
        val providerApiName = apiProviderName(record.providerId)

        return when {
            record.providerId == ThorChainProvider.id || record.providerId == MayaProvider.id -> TrackCall(
                Endpoint.Thorchain,
                UnstoppableAPI.Request.Track(
                    provider = providerApiName,
                    // Use the broadcast hash when available; fall back to depositAddress for memoless swaps.
                    inboundTxHash = record.transactionHash,
                    depositAddress = if (record.transactionHash == null) record.depositAddress else null,
                    fromAsset = record.fromAsset,
                    toAsset = record.toAsset,
                    toAddress = record.recipientAddress,
                )
            )

            // Our recorded swaps (every u_ USwap provider — P2P, NEAR, Barter, Circle).
            // Tracked by the record uuid alone; the server resolves the provider + all
            // details from it. inboundTxHash is required for DEX swaps and harmless for
            // P2P/NEAR (the server already holds their provider id and ignores it there).
            record.providerId.startsWith("u_") -> TrackCall(
                Endpoint.Recorded,
                UnstoppableAPI.Request.Track(
                    uuid = record.providerSwapId,
                    inboundTxHash = record.transactionHash,
                )
            )

            // Native single-tx EVM swaps — stateless on-chain reader.
            record.providerId == OneInchProvider.id ||
            record.providerId == UniswapV3Provider.id ||
            record.providerId == PancakeSwapV3Provider.id -> TrackCall(
                Endpoint.Evm,
                UnstoppableAPI.Request.Track(
                    provider = providerApiName,
                    hash = record.transactionHash,
                    chainId = evmChainIds[record.tokenInBlockchainTypeUid],
                    fromAsset = record.fromAsset,
                    toAsset = record.toAsset,
                    toAddress = record.recipientAddress,
                )
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
