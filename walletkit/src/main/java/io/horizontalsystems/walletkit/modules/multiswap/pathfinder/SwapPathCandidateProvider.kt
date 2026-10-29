package io.horizontalsystems.walletkit.modules.multiswap.pathfinder

import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.walletkit.core.defaultTokenQuery
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper

/**
 * One ordered source of intermediate-token candidates. [SwapPathFinder] walks its providers in
 * declared order and returns on the first candidate that links both legs, so the order in which a
 * provider returns its tokens IS the priority.
 */
interface ISwapPathCandidateProvider {
    fun candidates(tokenIn: Token, tokenOut: Token): List<Token>
}

/** The two coin-database lookups the candidate tiers need, so tests can run without MarketKit. */
interface ISwapPathTokenSource {
    fun tokens(queries: List<TokenQuery>): List<Token>
    fun fullCoins(coinUids: List<String>): List<FullCoin>
}

class MarketKitSwapPathTokenSource(private val marketKit: MarketKitWrapper) : ISwapPathTokenSource {
    override fun tokens(queries: List<TokenQuery>) = marketKit.tokens(queries)
    override fun fullCoins(coinUids: List<String>) = marketKit.fullCoins(coinUids)
}

/**
 * Tier 1: the native token of tokenOut's chain, then of tokenIn's chain.
 *
 * tokenOut first on purpose: reaching the destination chain is the binding constraint (for
 * `ETH -> <SPL token>`, SOL is the gateway).
 */
class NativeSwapPathCandidateProvider(
    private val tokenSource: ISwapPathTokenSource,
) : ISwapPathCandidateProvider {

    override fun candidates(tokenIn: Token, tokenOut: Token): List<Token> {
        // defaultTokenQuery, not a raw native query: BTC/LTC are derivation-based and BCH is
        // address-type-based, where a plain native query resolves to nothing.
        val queries = listOf(
            tokenOut.blockchainType.defaultTokenQuery,
            tokenIn.blockchainType.defaultTokenQuery,
        ).distinct()

        val tokens = try {
            tokenSource.tokens(queries)
        } catch (e: Throwable) {
            return emptyList()
        }

        // tokens(queries) does not promise input order; restore it.
        return queries.mapNotNull { query -> tokens.firstOrNull { it.tokenQuery == query } }
    }
}

/** Tier 2: every chain deployment of a fixed list of bridge coins, in list order. */
class CoinListSwapPathCandidateProvider(
    private val tokenSource: ISwapPathTokenSource,
    private val coinUids: List<String> = defaultCoinUids,
) : ISwapPathCandidateProvider {

    override fun candidates(tokenIn: Token, tokenOut: Token): List<Token> {
        val fullCoins = try {
            tokenSource.fullCoins(coinUids)
        } catch (e: Throwable) {
            return emptyList()
        }

        val byUid = fullCoins.associateBy { it.coin.uid }

        // fullCoins(coinUids) does not preserve input order; re-apply the configured priority.
        return coinUids.mapNotNull { byUid[it] }.flatMap { fullCoin ->
            sorted(fullCoin.tokens, tokenIn, tokenOut)
        }
    }

    // Within one coin: the deployment on tokenOut's chain first (for `ETH -> SPL` the USDT that
    // helps is USDT-on-Solana), then tokenIn's chain, then the rest in MarketKit order.
    private fun sorted(tokens: List<Token>, tokenIn: Token, tokenOut: Token): List<Token> {
        val outChain = mutableListOf<Token>()
        val inChain = mutableListOf<Token>()
        val rest = mutableListOf<Token>()

        for (token in tokens) {
            when (token.blockchainType) {
                tokenOut.blockchainType -> outChain.add(token)
                tokenIn.blockchainType -> inChain.add(token)
                else -> rest.add(token)
            }
        }

        return outChain + inChain + rest
    }

    companion object {
        /**
         * Bridge coins in priority order. MarketKit lists the bridged USDT deployments under
         * chain-specific coin uids rather than under `tether`, so each is named here or USDT on
         * that chain would never be a candidate.
         */
        val defaultCoinUids = listOf(
            "tether",
            "polygon-bridged-usdt-polygon",
            "binance-bridged-usdt-bnb-smart-chain",
            "l2-standard-bridged-usdt-base",
            "arbitrum-bridged-usdt-arbitrum",
            "usd-coin",
        )
    }
}
