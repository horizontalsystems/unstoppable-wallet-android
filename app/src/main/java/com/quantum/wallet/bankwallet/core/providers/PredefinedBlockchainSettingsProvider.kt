package com.quantum.wallet.bankwallet.core.providers

import com.quantum.wallet.bankwallet.core.managers.MoneroBirthdayProvider
import com.quantum.wallet.bankwallet.core.managers.RestoreSettings
import com.quantum.wallet.bankwallet.core.managers.RestoreSettingsManager
import com.quantum.wallet.bankwallet.core.managers.ZcashBirthdayProvider
import com.quantum.wallet.bankwallet.entities.Account
import io.horizontalsystems.marketkit.models.BlockchainType

class PredefinedBlockchainSettingsProvider(
    private val manager: RestoreSettingsManager,
    private val zcashBirthdayProvider: ZcashBirthdayProvider,
    private val moneroBirthdayProvider: MoneroBirthdayProvider
) {

    fun prepareNew(account: Account, blockchainType: BlockchainType) {
        val settings = RestoreSettings()
        when (blockchainType) {
            BlockchainType.Zcash -> {
                settings.birthdayHeight = zcashBirthdayProvider.getLatestCheckpointBlockHeight()
            }
            BlockchainType.Monero -> {
                settings.birthdayHeight = moneroBirthdayProvider.restoreHeightForNewWallet()
            }
            else -> {}
        }
        if (settings.isNotEmpty()) {
            manager.save(settings, account, blockchainType)
        }
    }
}
