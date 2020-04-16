package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class MainViewModel : ViewModel(), MainModule.IView, MainModule.IRouter {

    lateinit var delegate: MainModule.IViewDelegate
    val showRateAppLiveEvent = SingleLiveEvent<Unit>()
    val hideContentLiveData = MutableLiveData<Boolean>()

    fun init() {
        MainModule.init(this, this)
        delegate.viewDidLoad()
    }

    override fun showRateApp() {
        showRateAppLiveEvent.postValue(Unit)
    }

    override fun hideContent(hide: Boolean) {
        hideContentLiveData.postValue(hide)
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
