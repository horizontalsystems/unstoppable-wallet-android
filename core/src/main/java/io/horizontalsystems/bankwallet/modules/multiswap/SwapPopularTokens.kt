package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.defaultTokenQuery
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

/**
 * Builds the context-aware "Popular Tokens" list shown in the Swap token selector.
 *
 * The list is assembled from a fixed formula based on the opposite (context) token, then cleaned
 * up: nulls and the context token are dropped and duplicates removed (first occurrence kept).
 * See token_picker spec for the exact rules.
 *
 * This is the single source of truth for popular swap tokens — it also drives the default
 * `tokenIn` shown when the Swap screen is opened without history (see [SwapViewModel]).
 */
object SwapPopularTokens {

    fun build(marketKit: MarketKitWrapper, context: Token?): List<Token> {
        val baseNativeTypes = listOf(
            BlockchainType.Bitcoin,
            BlockchainType.Ethereum,
            BlockchainType.Monero,
            BlockchainType.Zcash,
            BlockchainType.Tron,
        )
        val natives = marketKit.tokens(baseNativeTypes.map { it.defaultTokenQuery })
            .associateBy { it.blockchainType }

        val usdtUids = listOf(
            "tether",
            "polygon-bridged-usdt-polygon",
            "binance-bridged-usdt-bnb-smart-chain",
            "l2-standard-bridged-usdt-base",
            "arbitrum-bridged-usdt-arbitrum",
        )
        val stableCoins = marketKit.fullCoins(usdtUids + "usd-coin")
        val usdt = stableCoins
            .filter { it.coin.uid in usdtUids }
            .sortedBy { usdtUids.indexOf(it.coin.uid) }
            .flatMap { it.tokens }
            .groupBy { it.blockchainType }
            .mapValues { it.value.first() }
        val usdc = stableCoins.firstOrNull { it.coin.uid == "usd-coin" }?.tokens
            ?.groupBy { it.blockchainType }?.mapValues { it.value.first() } ?: emptyMap()

        val usdtEth = usdt[BlockchainType.Ethereum]
        val usdcEth = usdc[BlockchainType.Ethereum]

        val baseNatives = baseNativeTypes.map { natives[it] }
        val tailStables = listOf(
            usdtEth,
            usdt[BlockchainType.Tron],
            usdt[BlockchainType.BinanceSmartChain],
            usdt[BlockchainType.Base],
        )

        val ordered: List<Token?> = when {
            context == null ->
                baseNatives + listOf(usdtEth, usdcEth) + tailStables

            context.type.isNative -> {
                // Case B — context is a native coin
                val usdtSame = usdt[context.blockchainType] ?: usdtEth
                val usdcSame = usdc[context.blockchainType] ?: usdcEth
                listOf(usdtSame, usdcSame) + tailStables + baseNatives
            }

            else -> {
                // Case A — context is a stablecoin or any other non-native token
                val nativeRaw = marketKit.token(context.blockchainType.defaultTokenQuery)
                // If the context chain's native coin is already a base native (e.g. ETH on an L2),
                // reuse that base-native token so it collapses into a single entry moved to the
                // front, instead of a chain-specific duplicate (ETH on Arbitrum + ETH on Ethereum).
                val nativeSame = baseNatives.firstOrNull { it?.coin?.uid == nativeRaw?.coin?.uid }
                    ?: nativeRaw
                val usdtSame = usdt[context.blockchainType] ?: usdtEth
                val usdcSame = usdc[context.blockchainType] ?: usdcEth
                listOf(nativeSame) + baseNatives + listOf(usdtSame, usdcSame) + tailStables
            }
        }

        val seenIds = mutableSetOf<String>()
        val result = mutableListOf<Token>()
        for (token in ordered) {
            if (token == null) continue
            // can't swap into the context token itself
            if (context != null &&
                token.coin.uid == context.coin.uid &&
                token.blockchainType == context.blockchainType
            ) continue
            if (!seenIds.add(token.tokenQuery.id)) continue
            result.add(token)
        }
        return result
    }
}
