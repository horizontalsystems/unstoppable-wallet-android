package io.horizontalsystems.bankwallet.modules.torpage

import io.horizontalsystems.core.SingleLiveEvent

class TorPageView: TorPageModule.IView {

    val setTorSwitch = SingleLiveEvent<Boolean>()

    override fun setTorSwitch(enabled: Boolean) {
        setTorSwitch.postValue(enabled)
    }
}
