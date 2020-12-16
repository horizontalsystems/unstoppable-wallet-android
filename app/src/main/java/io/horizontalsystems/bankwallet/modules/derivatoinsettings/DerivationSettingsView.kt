package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.core.SingleLiveEvent

class DerivationSettingsView : DerivationSettingsModule.IView {

    val showDerivationChangeAlert = SingleLiveEvent<Pair<DerivationSetting, String>>()
    val btcBipTitle = MutableLiveData<String>()
    val ltcBipTitle = MutableLiveData<String>()
    val btcBipVisibility = MutableLiveData<Boolean>()
    val ltcBipVisibility = MutableLiveData<Boolean>()
    val btcBipDerivation = MutableLiveData<AccountType.Derivation>()
    val ltcBipDerivation = MutableLiveData<AccountType.Derivation>()

    override fun setBtcBipVisibility(isVisible: Boolean) {
        btcBipVisibility.postValue(isVisible)
    }

    override fun setLtcBipVisibility(isVisible: Boolean) {
        ltcBipVisibility.postValue(isVisible)
    }

    override fun setBtcTitle(title: String) {
        btcBipTitle.postValue(title)
    }

    override fun setLtcTitle(title: String) {
        ltcBipTitle.postValue(title)
    }

    override fun setBtcBipSelection(selectedBip: AccountType.Derivation) {
        btcBipDerivation.postValue(selectedBip)
    }

    override fun setLtcBipSelection(selectedBip: AccountType.Derivation) {
        ltcBipDerivation.postValue(selectedBip)
    }

    override fun showDerivationChangeAlert(derivationSetting: DerivationSetting, coinTitle: String) {
        showDerivationChangeAlert.postValue(Pair(derivationSetting, coinTitle))
    }

}
