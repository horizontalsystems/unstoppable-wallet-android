package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.managers.RestoreSettingType
import io.horizontalsystems.walletkit.core.managers.RestoreSettingsManager
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet

// Enables the wallet that receives the bought token so the funds show up on Balance
// without a detour through Manage Wallets.
class SwapWalletEnabler(
    private val walletManager: WalletManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val newWalletSettingValue: (RestoreSettingType, BlockchainType) -> String? =
        restoreSettingsManager::getSettingValueForCreatedAccount,
) {

    // True when a wallet was enabled, false when it already was.
    fun enableReceivingWallet(account: Account, token: Token): Boolean {
        val wallet = Wallet(token, account)
        if (walletManager.activeWallets.contains(wallet)) return false

        // The funds arrive with this swap, so the wallet starts empty now.
        restoreSettingsManager.saveNewWalletSettings(account, token.blockchainType, newWalletSettingValue)
        walletManager.save(listOf(wallet))
        return true
    }
}
