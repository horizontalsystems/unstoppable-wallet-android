package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.core.SingleLiveEvent

class DerivationSettingsView : DerivationSettingsModule.IView {

    val showDerivationChangeAlert = SingleLiveEvent<Pair<DerivationSetting, String>>()
    val derivationSettings = MutableLiveData<List<DerivationSettingSectionViewItem>>()

    override fun setViewItems(viewItems: List<DerivationSettingSectionViewItem>) {
        derivationSettings.postValue(viewItems)
    }

    override fun showDerivationChangeAlert(derivationSetting: DerivationSetting, coinTitle: String) {
        showDerivationChangeAlert.postValue(Pair(derivationSetting, coinTitle))
    }

}
