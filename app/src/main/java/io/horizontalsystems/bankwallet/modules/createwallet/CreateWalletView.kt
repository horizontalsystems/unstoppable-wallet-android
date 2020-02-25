package io.horizontalsystems.bankwallet.modules.createwallet

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem

class CreateWalletView : CreateWalletModule.IView {
    val coinsLiveData = MutableLiveData<List<CoinManageViewItem>>()
    val createButtonEnabled = MutableLiveData<Boolean>()
    val showNotSupported = SingleLiveEvent<PredefinedAccountType>()

    override fun setItems(allCoinViewItems: List<CoinManageViewItem>) {
        coinsLiveData.postValue(allCoinViewItems)
    }

    override fun setCreateButton(enabled: Boolean) {
        createButtonEnabled.postValue(enabled)
    }

    override fun showNotSupported(predefinedAccountType: PredefinedAccountType) {
        showNotSupported.postValue(predefinedAccountType)
    }

}
