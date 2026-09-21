package io.horizontalsystems.walletkit.modules.addtoken

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.core.blockedEip20Addresses
import io.horizontalsystems.walletkit.core.nativeTokenContractAddress
import io.horizontalsystems.walletkit.core.order
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.entities.Wallet

class AddTokenService(
    private val coinManager: ICoinManager,
    private val walletManager: WalletManager,
    private val accountManager: IAccountManager,
    marketKit: MarketKitWrapper,
) {

    private val blockchainTypes = listOf(
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne,
        BlockchainType.Optimism,
        BlockchainType.Base,
        BlockchainType.ZkSync,
        BlockchainType.RobinhoodChain,
        BlockchainType.Arc,
    ) + ChainRegistry.all.filter { it.supportsCustomTokens }.map { it.blockchainType }

    val blockchains = marketKit
        .blockchains(blockchainTypes.map { it.uid })
        .sortedBy { it.type.order }

    val accountType = accountManager.activeAccount?.type

    suspend fun tokenInfo(blockchain: Blockchain, reference: String): TokenInfo? {
        if (reference.isEmpty()) return null

        val blockchainService = ChainRegistry[blockchain.type]?.addTokenBlockchainService(blockchain)
            ?: throw TokenError.InvalidReference

        if (!blockchainService.isValid(reference)) throw TokenError.InvalidReference

        if (reference.equals(blockchain.type.nativeTokenContractAddress, ignoreCase = true)) {
            val nativeToken = coinManager.getToken(TokenQuery(blockchain.type, TokenType.Native))
                ?: throw TokenError.NotFound
            return TokenInfo(nativeToken, true)
        }

        // Other synthetic transfer-log addresses (Arc's 0xfff…fe) are not contracts at all.
        if (blockchain.type.blockedEip20Addresses.any { it.equals(reference, ignoreCase = true) }) {
            throw TokenError.InvalidReference
        }

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
        val wallet = Wallet(token.token, account)
        walletManager.save(listOf(wallet))

        stat(page = StatPage.AddToken, event = StatEvent.AddToken(token.token))
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
