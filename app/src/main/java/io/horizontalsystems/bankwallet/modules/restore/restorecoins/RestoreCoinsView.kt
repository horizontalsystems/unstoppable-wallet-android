package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItem

class RestoreCoinsView : RestoreCoinsModule.IView {
    val coinsLiveData = MutableLiveData<Pair<List<CoinToggleViewItem>, List<CoinToggleViewItem>>>()
    val proceedButtonEnabled = MutableLiveData<Boolean>()
    val setTitle = MutableLiveData<PredefinedAccountType>()

    override fun setItems(featuredViewItems: List<CoinToggleViewItem>, viewItems: List<CoinToggleViewItem>) {
        coinsLiveData.postValue(Pair(featuredViewItems, viewItems))
    }

    override fun setProceedButton(enabled: Boolean) {
        proceedButtonEnabled.postValue(enabled)
    }

    override fun setTitle(predefinedAccountType: PredefinedAccountType) {
        setTitle.postValue(predefinedAccountType)
    }

}
