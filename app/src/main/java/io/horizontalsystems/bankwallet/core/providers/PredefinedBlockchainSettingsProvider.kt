package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.managers.MoneroBirthdayProvider
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.zanokit.ZanoKit
import java.util.Date

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
            BlockchainType.Zano -> {
                settings.birthdayHeight = ZanoKit.restoreHeightForDate(Date())
            }
            else -> {}
        }
        if (settings.isNotEmpty()) {
            manager.save(settings, account, blockchainType)
        }
    }
}
