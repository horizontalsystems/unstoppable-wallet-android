package io.horizontalsystems.bankwallet.modules.coinsettings

import io.horizontalsystems.bankwallet.core.ICoinSettingsManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

class CoinSettingsInteractor(private val coinSettingsManager: ICoinSettingsManager) : CoinSettingsModule.IInteractor {

    override fun bitcoinDerivation(): AccountType.Derivation {
        return coinSettingsManager.bitcoinDerivation
    }

    override fun syncMode(): SyncMode {
        return coinSettingsManager.syncMode
    }

    override fun updateBitcoinDerivation(derivation: AccountType.Derivation) {
        coinSettingsManager.bitcoinDerivation = derivation
    }

    override fun updateSyncMode(source: SyncMode) {
        coinSettingsManager.syncMode = source
    }
}
