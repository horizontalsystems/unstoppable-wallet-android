package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery

class WalletActivator(
    private val walletManager: WalletManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val token: (TokenQuery) -> Token?,
    private val newWalletSettingValue: (RestoreSettingType, BlockchainType) -> String? =
        restoreSettingsManager::getSettingValueForCreatedAccount,
) {

    fun activateWallets(account: Account, tokenQueries: List<TokenQuery>) {
        activateTokens(account, tokenQueries.mapNotNull(token))
    }

    fun activateTokens(account: Account, tokens: List<Token>) {
        // A created account has no history, so chains that sync from a birthday height
        // start at the current one. A restored account keeps whatever the user entered.
        if (account.origin == AccountOrigin.Created) {
            tokens.map { it.blockchainType }.distinct().forEach { blockchainType ->
                restoreSettingsManager.saveNewWalletSettings(account, blockchainType, newWalletSettingValue)
            }
        }

        walletManager.save(tokens.map { Wallet(it, account) })
    }

}
