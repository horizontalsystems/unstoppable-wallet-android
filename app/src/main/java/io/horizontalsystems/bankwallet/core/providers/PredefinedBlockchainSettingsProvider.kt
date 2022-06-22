package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.marketkit.models.CoinType

class PredefinedBlockchainSettingsProvider(
    private val manager: RestoreSettingsManager,
    private val zcashBirthdayProvider: ZcashBirthdayProvider
) {

    fun prepareNew(account: Account, coinType: CoinType) {
        val settings = RestoreSettings()
        when (coinType) {
            CoinType.Zcash -> {
                settings.birthdayHeight = zcashBirthdayProvider.getNearestBirthdayHeight()
            }
            else -> {}
        }
        if (settings.isNotEmpty()) {
            manager.save(settings, account, coinType)
        }
    }
}
