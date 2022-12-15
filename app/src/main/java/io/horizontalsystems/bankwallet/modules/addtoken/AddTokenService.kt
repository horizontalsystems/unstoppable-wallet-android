package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class AddTokenService(
    private val coinManager: ICoinManager,
    private val blockchainServices: List<IAddTokenBlockchainService>,
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager,
    private val marketKit: MarketKitWrapper
) {

    val accountType = accountManager.activeAccount?.type

    suspend fun getTokens(reference: String): List<TokenInfo> {
        if (reference.isEmpty()) return listOf()

        val validServices = blockchainServices.filter { it.isValid(reference) }

        if (validServices.isEmpty()) throw TokenError.InvalidReference

        val tokenInfos = mutableListOf<TokenInfo>()
        val activeWallets = walletManager.activeWallets
        validServices.forEach { service ->
            val token = coinManager.getToken(service.tokenQuery(reference))

            if (token != null && token.type !is TokenType.Unsupported) {
                val inWallet = activeWallets.any { it.token == token }
                val supported = isSupported(token.blockchainType)
                tokenInfos.add(TokenInfo.Local(token, inWallet, supported))
            } else {
                try {
                    val customCoin = service.customCoin(reference)
                    val supported = isSupported(customCoin.tokenQuery.blockchainType)
                    tokenInfos.add(TokenInfo.Remote(customCoin, supported))
                } catch (e: Exception) {
                }
            }
        }

        if (tokenInfos.isEmpty()) throw TokenError.NotFound

        return tokenInfos
    }

    fun addTokens(tokens: List<TokenInfo>) {
        val account = accountManager.activeAccount ?: return
        val platformCoins = tokens.mapNotNull { tokenInfo ->
            when (tokenInfo) {
                is TokenInfo.Local -> {
                    tokenInfo.token
                }
                is TokenInfo.Remote -> {
                    val tokenQuery = tokenInfo.tokenQuery
                    marketKit.blockchain(tokenQuery.blockchainType.uid)?.let { blockchain ->
                        val coinUid = tokenQuery.customCoinUid
                        Token(
                            coin = io.horizontalsystems.marketkit.models.Coin(coinUid, tokenInfo.coinName, tokenInfo.coinCode),
                            blockchain = blockchain,
                            type = tokenQuery.tokenType,
                            decimals = tokenInfo.decimals
                        )
                    }
                }
            }
        }

        val wallets = platformCoins.map { Wallet(it, account) }
        walletManager.save(wallets)
    }

    private fun isSupported(blockchainType: BlockchainType) =
        accountType?.let { blockchainType.supports(it) } ?: false

    sealed class TokenError : Exception() {
        object InvalidReference : TokenError()
        object NotFound : TokenError()
    }
}

sealed class TokenInfo {
    abstract val coinName: String
    abstract val coinCode: String
    abstract val decimals: Int
    abstract val tokenQuery: TokenQuery
    abstract val inWallet: Boolean
    abstract val supported: Boolean

    data class Local(
        val token: Token,
        override val inWallet: Boolean,
        override val supported: Boolean
    ) : TokenInfo() {
        override val coinName = token.coin.name
        override val coinCode = token.coin.code
        override val decimals = token.decimals
        override val tokenQuery = token.tokenQuery
    }

    data class Remote(
        val customCoin: AddTokenModule.CustomCoin,
        override val supported: Boolean
    ) : TokenInfo() {
        override val inWallet = false
        override val coinName = customCoin.name
        override val coinCode = customCoin.code
        override val decimals = customCoin.decimals
        override val tokenQuery = customCoin.tokenQuery
    }
}