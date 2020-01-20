package io.horizontalsystems.bankwallet.modules.coinsettings

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

class CoinSettingsView : CoinSettingsModule.IView {

    val selection = MutableLiveData<Pair<AccountType.Derivation, SyncMode>>()
    val showDerivationChangeAlert = SingleLiveEvent<AccountType.Derivation>()
    val showSyncModeChangeAlert = SingleLiveEvent<SyncMode>()

    override fun setSelection(derivation: AccountType.Derivation, syncMode: SyncMode) {
        selection.postValue(Pair(derivation, syncMode))
    }

    override fun showDerivationChangeAlert(derivation: AccountType.Derivation) {
        showDerivationChangeAlert.postValue(derivation)
    }

    override fun showSyncModeChangeAlert(syncMode: SyncMode) {
        showSyncModeChangeAlert.postValue(syncMode)
    }
}
