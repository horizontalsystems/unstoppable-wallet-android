package io.horizontalsystems.bankwallet.modules.coinsettings

import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.SyncMode

class CoinSettingsView : CoinSettingsModule.IView {

    val syncModeLiveEvent = SingleLiveEvent<SyncMode>()
    val derivationLiveEvent = SingleLiveEvent<Derivation>()

    override fun update(syncMode: SyncMode) {
        syncModeLiveEvent.value = syncMode
    }

    override fun update(derivation: Derivation) {
        derivationLiveEvent.postValue(derivation)
    }

}
