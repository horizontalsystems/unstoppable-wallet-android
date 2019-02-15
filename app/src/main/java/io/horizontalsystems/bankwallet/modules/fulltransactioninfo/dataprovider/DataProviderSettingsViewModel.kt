package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class DataProviderSettingsViewModel : ViewModel(), DataProviderSettingsModule.View {

    lateinit var delegate: DataProviderSettingsModule.ViewDelegate

    val providerItems = MutableLiveData<List<DataProviderSettingsItem>>()
    val closeLiveEvent = SingleLiveEvent<Unit>()

    fun init(coinCode: CoinCode, transactionHash: String) {
        DataProviderSettingsModule.init(coinCode, transactionHash, this)
        delegate.viewDidLoad()
    }

    override fun show(items: List<DataProviderSettingsItem>) {
        providerItems.value = items
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
