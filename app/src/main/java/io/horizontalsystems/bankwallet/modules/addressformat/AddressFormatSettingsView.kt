package io.horizontalsystems.bankwallet.modules.addressformat

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.core.SingleLiveEvent

class AddressFormatSettingsView : AddressFormatSettingsModule.IView {

    val showDerivationChangeAlert = SingleLiveEvent<Pair<DerivationSetting, String>>()
    val btcBipTitle = MutableLiveData<String>()
    val ltcBipTitle = MutableLiveData<String>()
    val btcBipEnabled = MutableLiveData<Boolean>()
    val ltcBipEnabled = MutableLiveData<Boolean>()
    val btcBipDerivation = MutableLiveData<AccountType.Derivation>()
    val ltcBipDerivation = MutableLiveData<AccountType.Derivation>()

    override fun setBtcTitle(title: String) {
        btcBipTitle.postValue(title)
    }

    override fun setLtcTitle(title: String) {
        ltcBipTitle.postValue(title)
    }

    override fun setBtcBipsEnabled(enabled: Boolean) {
        btcBipEnabled.postValue(enabled)
    }

    override fun setBtcBipSelection(selectedBip: AccountType.Derivation) {
        btcBipDerivation.postValue(selectedBip)
    }

    override fun setLtcBipsEnabled(enabled: Boolean) {
        ltcBipEnabled.postValue(enabled)
    }

    override fun setLtcBipSelection(selectedBip: AccountType.Derivation) {
        ltcBipDerivation.postValue(selectedBip)
    }

    override fun showDerivationChangeAlert(derivationSetting: DerivationSetting, coinTitle: String) {
        showDerivationChangeAlert.postValue(Pair(derivationSetting, coinTitle))
    }

}
