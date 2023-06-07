package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType

class AddTokenService(
    private val coinManager: ICoinManager,
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager,
    marketKit: MarketKitWrapper,
) {

    private val blockchainTypes = listOf(
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Tron,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.BinanceChain,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne,
        BlockchainType.Optimism,
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
        val wallet = Wallet(token.token, account)
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
