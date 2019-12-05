package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem

class RestoreCoinsView : RestoreCoinsModule.IView {
    val coinsLiveData = MutableLiveData<List<CoinManageViewItem>>()
    val proceedButtonEnabled = MutableLiveData<Boolean>()
    val setTitle = MutableLiveData<PredefinedAccountType>()

    override fun setItems(coinViewItems: List<CoinManageViewItem>) {
        coinsLiveData.postValue(coinViewItems)
    }

    override fun setProceedButton(enabled: Boolean) {
        proceedButtonEnabled.postValue(enabled)
    }

    override fun setTitle(predefinedAccountType: PredefinedAccountType) {
        setTitle.postValue(predefinedAccountType)
    }

}
