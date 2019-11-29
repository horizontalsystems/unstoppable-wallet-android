package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItem

class RestoreCoinsView : RestoreCoinsModule.IView {
    val featuredCoinsLiveData = MutableLiveData<List<CoinToggleViewItem>>()
    val coinsLiveData = MutableLiveData<List<CoinToggleViewItem>>()
    val proceedButtonEnabled = MutableLiveData<Boolean>()
    val setTitle = MutableLiveData<PredefinedAccountType>()
    val errorLiveEvent = SingleLiveEvent<Exception>()

    override fun setItems(featuredViewItems: List<CoinToggleViewItem>, viewItems: List<CoinToggleViewItem>) {
        featuredCoinsLiveData.postValue(featuredViewItems)
        coinsLiveData.postValue(viewItems)
    }

    override fun setProceedButton(enabled: Boolean) {
        proceedButtonEnabled.postValue(enabled)
    }

    override fun setTitle(predefinedAccountType: PredefinedAccountType) {
        setTitle.postValue(predefinedAccountType)
    }

    override fun showError(exception: Exception) {
        errorLiveEvent.postValue(exception)
    }
}
