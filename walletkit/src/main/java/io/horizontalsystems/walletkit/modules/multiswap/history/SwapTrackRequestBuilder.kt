package io.horizontalsystems.walletkit.modules.multiswap.history

import io.horizontalsystems.walletkit.modules.multiswap.providers.PANCAKE_V3_PROVIDER_ID
import io.horizontalsystems.walletkit.modules.multiswap.providers.UNISWAP_V3_PROVIDER_ID
import io.horizontalsystems.walletkit.modules.multiswap.providers.ONEINCH_PROVIDER_ID
import io.horizontalsystems.walletkit.modules.multiswap.providers.MAYA_PROVIDER_ID
import io.horizontalsystems.walletkit.modules.multiswap.providers.THORCHAIN_PROVIDER_ID
import io.horizontalsystems.walletkit.entities.SwapRecord
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderIds
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI

object SwapTrackRequestBuilder {

    // The track endpoint a record is tracked through.
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
        // App-resolved sending address: when broadcasts are bundled/relayed, the
        // on-chain `from` is an operator account, not the user's — the app knows better.
        val fromAddress = SwapTrackEnrichmentResolver.provider?.fromAddress(record)

        return when {
            record.providerId == THORCHAIN_PROVIDER_ID || record.providerId == MAYA_PROVIDER_ID -> TrackCall(
                Endpoint.Thorchain,
                UnstoppableAPI.Request.Track(
                    provider = providerApiName,
                    // Use the broadcast hash when available; fall back to depositAddress for memoless swaps.
                    inboundTxHash = record.transactionHash,
                    depositAddress = if (record.transactionHash == null) record.depositAddress else null,
                    fromAsset = record.fromAsset,
                    fromAddress = fromAddress,
                    toAsset = record.toAsset,
                    toAddress = record.recipientAddress,
                )
            )

            // Native single-tx EVM swaps — stateless on-chain reader.
            record.providerId == ONEINCH_PROVIDER_ID ||
            record.providerId == UNISWAP_V3_PROVIDER_ID ||
            record.providerId == PANCAKE_V3_PROVIDER_ID -> TrackCall(
                Endpoint.Evm,
                UnstoppableAPI.Request.Track(
                    provider = providerApiName,
                    hash = record.transactionHash,
                    chainId = evmChainIds[record.tokenInBlockchainTypeUid],
                    fromAsset = record.fromAsset,
                    fromAddress = fromAddress,
                    toAsset = record.toAsset,
                    toAddress = record.recipientAddress,
                )
            )

            // Our recorded swaps: every order the server created, tracked by the record uuid
            // alone; the server resolves the provider + all details from it, and rejects any
            // other field. The inbound tx hash reaches the server through /v3/signed from the
            // send path, never from here.
            else -> TrackCall(
                Endpoint.Recorded,
                UnstoppableAPI.Request.Track(uuid = record.providerSwapId)
            )
        }
    }

    private fun apiProviderName(providerId: String): String = when (providerId) {
        THORCHAIN_PROVIDER_ID -> "THORCHAIN"
        MAYA_PROVIDER_ID -> "MAYACHAIN"
        ONEINCH_PROVIDER_ID -> "ONEINCH"
        PANCAKE_V3_PROVIDER_ID -> "PANCAKESWAP"
        UNISWAP_V3_PROVIDER_ID -> "UNISWAP_V3"
        else -> SwapProviderIds.serverId(providerId)
    }
}

