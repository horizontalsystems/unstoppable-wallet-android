package io.horizontalsystems.bankwallet.modules.createwallet

import androidx.lifecycle.MutableLiveData

class CreateWalletView : CreateWalletModule.IView {
    val itemsLiveData = MutableLiveData<List<CreateWalletModule.CoinViewItem>>()
    val createEnabledLiveData = MutableLiveData<Boolean>()

    override fun setItems(items: List<CreateWalletModule.CoinViewItem>) {
        itemsLiveData.postValue(items)
    }

    override fun setCreateEnabled(enabled: Boolean) {
        createEnabledLiveData.postValue(enabled)
    }
}
