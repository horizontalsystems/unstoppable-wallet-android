package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.supports
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType

class AddTokenService(
    private val coinManager: ICoinManager,
    private val blockchainServices: List<IAddTokenBlockchainService>,
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager,
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
                tokenInfos.add(TokenInfo(token, inWallet, supported))
            } else {
                try {
                    val customToken = service.token(reference)
                    val supported = isSupported(customToken.blockchainType)
                    tokenInfos.add(TokenInfo(customToken, false, supported))
                } catch (e: Exception) {
                }
            }
        }

        if (tokenInfos.isEmpty()) throw TokenError.NotFound

        return tokenInfos
    }

    fun addTokens(tokens: List<TokenInfo>) {
        val account = accountManager.activeAccount ?: return
        val wallets = tokens.map { Wallet(it.token, account) }
        walletManager.save(wallets)
    }

    private fun isSupported(blockchainType: BlockchainType) =
        accountType?.let { blockchainType.supports(it) } ?: false

    sealed class TokenError : Exception() {
        object InvalidReference : TokenError()
        object NotFound : TokenError()
    }

    data class TokenInfo(
        val token: Token,
        val inWallet: Boolean,
        val supported: Boolean
    )
}
