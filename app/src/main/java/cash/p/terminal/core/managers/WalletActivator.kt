package cash.p.terminal.core.managers

import cash.p.terminal.core.IWalletManager
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.ConfiguredToken
import cash.p.terminal.entities.Wallet
import io.horizontalsystems.marketkit.models.TokenQuery

class WalletActivator(
    private val walletManager: IWalletManager,
    private val marketKit: MarketKitWrapper,
) {

    fun activateWallets(account: Account, tokenQueries: List<TokenQuery>) {
        val wallets = tokenQueries.mapNotNull { tokenQuery ->
            marketKit.token(tokenQuery)?.let { token ->
                Wallet(token, account)
            }
        }

        walletManager.save(wallets)
    }

    fun activateConfiguredTokens(account: Account, configuredTokens: List<ConfiguredToken>) {
        val wallets = mutableListOf<Wallet>()

        for (configuredToken in configuredTokens) {
            wallets.add(Wallet(configuredToken, account))
        }

        walletManager.save(wallets)
    }

}
