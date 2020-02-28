package io.horizontalsystems.bankwallet.modules.tor

import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.core.SingleLiveEvent

class TorStatusView: TorStatusModule.View {

    val torConnectionStatus = SingleLiveEvent<TorStatus>()

    override fun updateConnectionStatus(connectionStatus: TorStatus) {
        torConnectionStatus.postValue(connectionStatus)
    }
}