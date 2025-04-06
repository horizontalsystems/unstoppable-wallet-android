package cash.p.terminal.modules.addtoken

import cash.p.terminal.core.App
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.core.order
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.Blockchain
import cash.p.terminal.wallet.entities.TokenType

class AddTokenService(
    private val coinManager: ICoinManager,
    private val walletManager: IWalletManager,
    private val accountManager: cash.p.terminal.wallet.IAccountManager,
    marketKit: MarketKitWrapper,
) {

    private val blockchainTypes = listOf(
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Tron,
        BlockchainType.Ton,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.BinanceChain,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne,
        BlockchainType.Optimism,
        BlockchainType.Base,
        BlockchainType.ZkSync,
        BlockchainType.Solana
    )

    val blockchains = marketKit
        .blockchains(blockchainTypes.map { it.uid })
        .sortedBy { it.type.order }

    val accountType = accountManager.activeAccount?.type

    suspend fun tokenInfo(blockchain: Blockchain, reference: String): TokenInfo? {
        if (reference.isEmpty()) return null

        val blockchainService = when (blockchain.type) {
            BlockchainType.BinanceChain -> AddBep2TokenBlockchainService(
                blockchain,
                App.networkManager
            )
            BlockchainType.Tron -> {
                AddTronTokenBlockchainService.getInstance(blockchain)
            }
            BlockchainType.Ton -> {
                AddTonTokenBlockchainService(blockchain)
            }
            BlockchainType.Solana -> {
                AddSolanaTokenBlockchainService.getInstance(blockchain)
            }
            else -> AddEvmTokenBlockchainService.getInstance(blockchain)
        }

        if (!blockchainService.isValid(reference)) throw TokenError.InvalidReference

        val token = coinManager.getToken(blockchainService.tokenQuery(reference))
        if (token != null && token.type !is TokenType.Unsupported) {
            return TokenInfo(token, true)
        }

        try {
            val customToken = blockchainService.token(reference)
            return TokenInfo(customToken, false)
        } catch (e: Throwable) {
            throw TokenError.NotFound
        }
    }

    fun addToken(token: TokenInfo) {
        val account = accountManager.activeAccount ?: return
        val wallet = cash.p.terminal.wallet.Wallet(token.token, account)
        walletManager.save(listOf(wallet))
    }

    sealed class TokenError : Exception() {
        object InvalidReference : TokenError()
        object NotFound : TokenError()
    }

    data class TokenInfo(
        val token: Token,
        val inCoinList: Boolean,
    )
}
