package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class MainViewModel : ViewModel(), MainModule.IView {

    lateinit var delegate: MainModule.IViewDelegate
    val showRateAppLiveEvent = SingleLiveEvent<Unit>()
    val openPlayMarketLiveEvent = SingleLiveEvent<Unit>()
    val hideContentLiveData = MutableLiveData<Boolean>()
    val setBadgeVisibleLiveData = MutableLiveData<Boolean>()
    val transactionTabEnabledLiveData = MutableLiveData<Boolean>()

    fun init() {
        MainModule.init(this)
        delegate.viewDidLoad()
    }

    override fun showRateApp() {
        showRateAppLiveEvent.postValue(Unit)
    }

    override fun openPlayMarket() {
        openPlayMarketLiveEvent.postValue(Unit)
    }

    override fun hideContent(hide: Boolean) {
        hideContentLiveData.postValue(hide)
    }

    override fun toggleBagdeVisibility(visible: Boolean) {
        setBadgeVisibleLiveData.postValue(visible)
    }

    override fun setTransactionTabEnabled(enabled: Boolean) {
        transactionTabEnabledLiveData.postValue(enabled)
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
