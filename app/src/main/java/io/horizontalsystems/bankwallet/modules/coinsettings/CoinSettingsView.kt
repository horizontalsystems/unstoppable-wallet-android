package io.horizontalsystems.bankwallet.modules.coinsettings

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.SyncMode

class CoinSettingsView : CoinSettingsModule.IView {

    val syncModeLiveEvent = SingleLiveEvent<Pair<SyncMode, String>>()
    val derivationLiveEvent = SingleLiveEvent<Derivation>()
    val titleData = MutableLiveData<String>()

    override fun setTitle(title: String) {
        titleData.postValue(title)
    }

    override fun update(syncMode: SyncMode, coinTitle: String) {
        syncModeLiveEvent.value = Pair(syncMode, coinTitle)
    }

    override fun update(derivation: Derivation) {
        derivationLiveEvent.postValue(derivation)
    }

}
