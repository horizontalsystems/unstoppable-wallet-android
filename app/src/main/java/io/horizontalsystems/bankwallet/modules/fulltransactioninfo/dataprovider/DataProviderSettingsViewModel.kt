package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin

class DataProviderSettingsViewModel : ViewModel(), DataProviderSettingsModule.View {

    lateinit var delegate: DataProviderSettingsModule.ViewDelegate

    val providerItems = MutableLiveData<List<DataProviderSettingsItem>>()
    val closeLiveEvent = SingleLiveEvent<Unit>()

    fun init(coin: Coin) {
        DataProviderSettingsModule.init(coin,this)
        delegate.viewDidLoad()
    }

    override fun show(items: List<DataProviderSettingsItem>) {
        providerItems.value = items
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
