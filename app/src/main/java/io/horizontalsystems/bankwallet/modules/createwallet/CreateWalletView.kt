package io.horizontalsystems.bankwallet.modules.createwallet

import androidx.lifecycle.MutableLiveData

class CreateWalletView : CreateWalletModule.IView {
    val itemsLiveData = MutableLiveData<List<CreateWalletModule.CoinViewItem>>()

    override fun setItems(items: List<CreateWalletModule.CoinViewItem>) {
        itemsLiveData.postValue(items)
    }
}
