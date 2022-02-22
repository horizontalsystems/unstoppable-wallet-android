package io.horizontalsystems.bankwallet.modules.walletconnect.list.v2

import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager

class WC2ListService(
    private val sessionManager: WC2SessionManager
) {

    val sessions by sessionManager::sessions
    val sessionsObservable by sessionManager::sessionsObservable
    val pendingRequestsObservable by sessionManager::pendingRequestsObservable

    val pendingRequestsCount: Int
        get() = sessionManager.pendingRequests().size

}
