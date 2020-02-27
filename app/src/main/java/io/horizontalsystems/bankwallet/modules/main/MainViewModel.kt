package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class MainViewModel : ViewModel(), MainModule.IView, MainModule.IRouter {

    val torConnectionStatus = SingleLiveEvent<Unit>()
    lateinit var delegate: MainModule.IViewDelegate

    fun init() {
        MainModule.init(this, this)
        delegate.viewDidLoad()
    }

    override fun showTorConnectionStatus() {
        torConnectionStatus.call()
    }
}
