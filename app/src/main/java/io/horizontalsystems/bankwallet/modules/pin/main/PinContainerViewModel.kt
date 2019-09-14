package io.horizontalsystems.bankwallet.modules.pin.main

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class PinContainerViewModel: ViewModel(), PinContainerModule.IView, PinContainerModule.IRouter {

    val closeApplicationLiveEvent = SingleLiveEvent<Unit>()
    val closeActivityLiveEvent = SingleLiveEvent<Unit>()

    fun init(showCancelButton: Boolean) {
        PinContainerModule.init(this, this, showCancelButton)
    }

    lateinit var delegate: PinContainerModule.IViewDelegate

    override fun closeActivity() {
        closeActivityLiveEvent.call()
    }

    override fun closeApplication() {
        closeApplicationLiveEvent.call()
    }
}
