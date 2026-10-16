package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.managers.RestoreSettingType
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.RestoreSettingsManager
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.core.restoreSettingTypes
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

        saveDefaultRestoreSettings(account, token.blockchainType)
        walletManager.save(listOf(wallet))
        return true
    }

    // Chains that sync from a birthday height get the "wallet is new" value for every
    // setting the account has not saved yet: the funds arrive with this swap, so there is
    // nothing older to scan for, and a missing height would mean a scan from the first block.
    private fun saveDefaultRestoreSettings(account: Account, blockchainType: BlockchainType) {
        val existing = restoreSettingsManager.settings(account, blockchainType)
        val defaults = RestoreSettings()

        blockchainType.restoreSettingTypes
            .filter { existing[it] == null }
            .forEach { settingType ->
                newWalletSettingValue(settingType, blockchainType)?.let { defaults[settingType] = it }
            }

        if (defaults.isNotEmpty()) {
            // reload = false: the wallet is saved right after, which creates the adapter
            // with these settings; a reload here would rebuild that adapter a second time.
            restoreSettingsManager.save(defaults, account, blockchainType, reload = false)
        }
    }
}
