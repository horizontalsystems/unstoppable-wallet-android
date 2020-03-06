package io.horizontalsystems.bankwallet.modules.torpage

import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.core.SingleLiveEvent

class TorPageView: TorPageModule.IView {

    val setTorSwitch = SingleLiveEvent<Boolean>()
    val setTorConnectionStatus = SingleLiveEvent<TorStatus>()
    val notificationsNotEnabledAlert = SingleLiveEvent<Unit>()

    override fun setTorSwitch(enabled: Boolean) {
        setTorSwitch.postValue(enabled)
    }

    override fun setConnectionStatus(connectionStatus: TorStatus) {
        setTorConnectionStatus.postValue(connectionStatus)
    }

    override fun showNotificationsNotEnabledAlert() {
        notificationsNotEnabledAlert.call()
    }
}
