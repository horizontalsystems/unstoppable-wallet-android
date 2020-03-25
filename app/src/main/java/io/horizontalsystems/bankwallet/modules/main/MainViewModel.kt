package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class MainViewModel : ViewModel(), MainModule.IView, MainModule.IRouter {

    lateinit var delegate: MainModule.IViewDelegate
    val showRateAppLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        MainModule.init(this, this)
        delegate.viewDidLoad()
    }

    override fun showRateApp() {
        showRateAppLiveEvent.postValue(Unit)
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
