package io.horizontalsystems.bankwallet.modules.createwallet

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItem

class CreateWalletView : CreateWalletModule.IView {
    val coinsLiveData = MutableLiveData<Pair<List<CoinToggleViewItem>, List<CoinToggleViewItem>>>()
    val createButtonEnabled = MutableLiveData<Boolean>()
    val errorLiveEvent = SingleLiveEvent<Exception>()

    override fun setItems(featuredViewItems: List<CoinToggleViewItem>, viewItems: List<CoinToggleViewItem>) {
        coinsLiveData.postValue(Pair(featuredViewItems, viewItems))
    }

    override fun setCreateButton(enabled: Boolean) {
        createButtonEnabled.postValue(enabled)
    }

    override fun showError(exception: Exception) {
        errorLiveEvent.postValue(exception)
    }
}
