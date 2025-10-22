package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery

class WalletActivator(
    private val walletManager: IWalletManager,
    private val marketKit: MarketKitWrapper,
) {

    fun activateWallets(account: Account, tokenQueries: List<TokenQuery>) {
        val wallets = tokenQueries.mapNotNull { tokenQuery ->
            val token = marketKit.token(tokenQuery)
            
            // Debug logging
            if (tokenQuery.blockchainType is io.horizontalsystems.marketkit.models.BlockchainType.Unsupported && 
                tokenQuery.blockchainType.uid == "oxyra") {
                println("🔍 WalletActivator - Oxyra TokenQuery: $tokenQuery")
                println("🔍 WalletActivator - Token: $token")
            }
            
            token?.let { Wallet(it, account) }
        }

        println("🔍 WalletActivator - Created ${wallets.size} wallets for account ${account.name}")
        walletManager.save(wallets)
    }

    fun activateTokens(account: Account, tokens: List<Token>) {
        val wallets = mutableListOf<Wallet>()

        for (token in tokens) {
            wallets.add(Wallet(token, account))
        }

        walletManager.save(wallets)
    }

}
