package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.SyncMode

class BlockchainSettingsView : BlockchainSettingsModule.IView {

    val showDerivationChangeAlert = SingleLiveEvent<Pair<AccountType.Derivation, String>>()
    val showSyncModeChangeAlert = SingleLiveEvent<Pair<SyncMode, String>>()
    val derivationLiveEvent = MutableLiveData<AccountType.Derivation>()
    val syncModeLiveEvent = MutableLiveData<SyncMode>()
    val sourceLinkLiveEvent = MutableLiveData<CoinType>()
    val titleLiveEvent = SingleLiveEvent<String>()

    override fun setTitle(title: String) {
        titleLiveEvent.postValue(title)
    }

    override fun showDerivationChangeAlert(derivation: AccountType.Derivation, coinTitle: String) {
        showDerivationChangeAlert.postValue(Pair(derivation, coinTitle))
    }

    override fun showSyncModeChangeAlert(syncMode: SyncMode, coinTitle: String) {
        showSyncModeChangeAlert.postValue(Pair(syncMode, coinTitle))
    }

    override fun setDerivation(derivation: AccountType.Derivation) {
        derivationLiveEvent.postValue(derivation)
    }

    override fun setSyncMode(syncMode: SyncMode) {
        syncModeLiveEvent.postValue(syncMode)
    }

    override fun setSourceLink(coinType: CoinType) {
        sourceLinkLiveEvent.postValue(coinType)
    }
}
