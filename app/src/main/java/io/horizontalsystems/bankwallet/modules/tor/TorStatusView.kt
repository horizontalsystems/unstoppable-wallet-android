package io.horizontalsystems.bankwallet.modules.tor

import io.horizontalsystems.bankwallet.modules.settings.security.tor.TorStatus
import io.horizontalsystems.core.SingleLiveEvent

class TorStatusView: TorStatusModule.View {

    val torConnectionStatus = SingleLiveEvent<TorStatus>()

    override fun updateConnectionStatus(connectionStatus: TorStatus) {
        torConnectionStatus.postValue(connectionStatus)
    }
}