package io.horizontalsystems.walletkit.modules.multiswap.pathfinder

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.walletkit.modules.multiswap.SwapQuote
import io.horizontalsystems.walletkit.modules.multiswap.providers.CanonicalAssetId
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderType
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapSuspension
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapSuspensionIndex
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import java.math.BigDecimal

object SwapPathFixtures {
    val eth = token("ethereum", BlockchainType.Ethereum)
    val sol = token("solana", BlockchainType.Solana)
    val trx = token("tron", BlockchainType.Tron)
    val bonk = token("bonk", BlockchainType.Solana, TokenType.Spl("bonk-mint"))

    val usdtCoin = Coin("tether", "Tether", "USDT")
    val usdtEth = Token(usdtCoin, blockchain(BlockchainType.Ethereum), TokenType.Eip20("0xusdt-eth"), 6)
    val usdtTron = Token(usdtCoin, blockchain(BlockchainType.Tron), TokenType.Eip20("usdt-tron"), 6)
    val usdtSol = Token(usdtCoin, blockchain(BlockchainType.Solana), TokenType.Spl("usdt-sol"), 6)

    val usdcCoin = Coin("usd-coin", "USD Coin", "USDC")
    val usdcEth = Token(usdcCoin, blockchain(BlockchainType.Ethereum), TokenType.Eip20("0xusdc-eth"), 6)

    fun token(coinUid: String, blockchainType: BlockchainType, type: TokenType = TokenType.Native) = Token(
        coin = Coin(coinUid, coinUid, coinUid.uppercase()),
        blockchain = blockchain(blockchainType),
        type = type,
        decimals = 8,
    )

    fun blockchain(type: BlockchainType) = Blockchain(type, type.uid, null)

    fun edge(from: Token, to: Token) = "${from.tokenQuery.id}>${to.tokenQuery.id}"

    fun provider(id: String = "stub", edges: List<String>, log: MutableList<String>? = null): IMultiSwapProvider =
        StubProvider(id, edges.toSet(), log)

    fun candidates(vararg tokens: Token) = object : ISwapPathCandidateProvider {
        override fun candidates(tokenIn: Token, tokenOut: Token) = tokens.toList()
    }

    fun suspensions(providerId: String, sell: Token, buy: Token) = SwapSuspensionIndex(
        rulesByProvider = mapOf(
            SwapSuspensionIndex.serverId(providerId) to listOf(
                SwapSuspension(
                    kind = SwapSuspension.Kind.Pair,
                    sellAsset = CanonicalAssetId.of(sell),
                    buyAsset = CanonicalAssetId.of(buy),
                )
            )
        )
    )

    fun stubTokenSource() = StubTokenSource().apply {
        for (token in listOf(eth, sol, trx)) {
            tokensByQuery[token.tokenQuery] = token
        }
        coins = listOf(
            FullCoin(usdtCoin, listOf(usdtEth, usdtTron, usdtSol)),
            FullCoin(usdcCoin, listOf(usdcEth)),
        )
    }

    class StubTokenSource : ISwapPathTokenSource {
        val tokensByQuery = mutableMapOf<TokenQuery, Token>()
        var coins = listOf<FullCoin>()
        var fullCoinsThrows = false

        override fun tokens(queries: List<TokenQuery>): List<Token> =
            // deliberately reversed: the provider must restore the requested order itself
            queries.reversed().mapNotNull { tokensByQuery[it] }

        override fun fullCoins(coinUids: List<String>): List<FullCoin> {
            if (fullCoinsThrows) throw IllegalStateException("stub")
            return coins.filter { it.coin.uid in coinUids }
        }
    }

    private class StubProvider(
        override val id: String,
        private val edges: Set<String>,
        private val log: MutableList<String>?,
    ) : IMultiSwapProvider {
        override val title = id
        override val type = SwapProviderType.DEX
        override val requireTerms = false

        override fun isSingleTransactionSwap(tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String) = true

        override fun supports(blockchainType: BlockchainType) = true

        override fun supports(tokenFrom: Token, tokenTo: Token): Boolean {
            val edge = edge(tokenFrom, tokenTo)
            log?.add(edge)
            return edge in edges
        }

        override suspend fun fetchQuote(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal): SwapQuote =
            error("not used")

        override suspend fun fetchFinalQuote(
            tokenIn: Token,
            tokenOut: Token,
            amountIn: BigDecimal,
            sendTransactionSettings: SendTransactionSettings?,
            swapQuote: SwapQuote,
            recipient: Address?,
            slippage: BigDecimal,
        ): SwapFinalQuote = error("not used")
    }
}
