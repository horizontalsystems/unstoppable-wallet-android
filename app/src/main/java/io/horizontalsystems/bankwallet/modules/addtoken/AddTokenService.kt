package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.customCoinUid
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.Platform
import io.horizontalsystems.marketkit.models.PlatformCoin

class AddTokenService(
    private val coinManager: ICoinManager,
    private val blockchainServices: List<IAddTokenBlockchainService>,
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager
) {

    suspend fun getTokens(reference: String): List<TokenInfo> {
        if (reference.isEmpty()) return listOf()

        val validServices = blockchainServices.filter { it.isValid(reference) }

        if (validServices.isEmpty()) throw TokenError.InvalidReference

        val tokenInfos = mutableListOf<TokenInfo>()
        val activeWallets = walletManager.activeWallets
        validServices.forEach { service ->
            val platformCoin = coinManager.getPlatformCoin(service.coinType(reference))

            if (platformCoin != null) {
                val inWallet = activeWallets.any { it.platformCoin == platformCoin }
                tokenInfos.add(TokenInfo.Local(platformCoin, inWallet))
            } else {
                try {
                    val customCoin = service.customCoin(reference)
                    tokenInfos.add(TokenInfo.Remote(customCoin))
                } catch (e: Exception) {
                }
            }
        }

        if (tokenInfos.isEmpty()) throw TokenError.NotFound

        return tokenInfos
    }

    fun addTokens(tokens: List<TokenInfo>) {
        val account = accountManager.activeAccount ?: return
        val platformCoins = tokens.map { tokenInfo ->
            when (tokenInfo) {
                is TokenInfo.Local -> {
                    tokenInfo.platformCoin
                }
                is TokenInfo.Remote -> {
                    val coinType = tokenInfo.coinType
                    val coinUid = coinType.customCoinUid
                    PlatformCoin(
                        Platform(coinType, tokenInfo.decimals, coinUid),
                        Coin(coinUid, tokenInfo.coinName, tokenInfo.coinCode)
                    )
                }
            }
        }

        val wallets = platformCoins.map { Wallet(it, account) }
        walletManager.save(wallets)
    }

    sealed class TokenError : Exception() {
        object InvalidReference : TokenError()
        object NotFound : TokenError()
    }
}

sealed class TokenInfo {
    abstract val coinName: String
    abstract val coinCode: String
    abstract val decimals: Int
    abstract val coinType: CoinType
    abstract val inWallet: Boolean

    data class Local(val platformCoin: PlatformCoin, override val inWallet: Boolean) : TokenInfo() {
        override val coinName = platformCoin.coin.name
        override val coinCode = platformCoin.coin.code
        override val decimals = platformCoin.decimals
        override val coinType = platformCoin.coinType
    }

    data class Remote(val customCoin: AddTokenModule.CustomCoin) : TokenInfo() {
        override val inWallet = false
        override val coinName = customCoin.name
        override val coinCode = customCoin.code
        override val decimals = customCoin.decimals
        override val coinType = customCoin.type
    }
}