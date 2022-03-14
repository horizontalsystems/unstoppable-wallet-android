package io.horizontalsystems.bankwallet.modules.walletconnect.list.v2

import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager

class WC2ListService(
    private val sessionManager: WC2SessionManager
) {
    fun delete(sessionId: String) {
        sessionManager.deleteSession(sessionId)
    }

    val sessions by sessionManager::sessions
    val sessionsObservable by sessionManager::sessionsObservable
    val pendingRequestsObservable by sessionManager::pendingRequestCountObservable

    val pendingRequestsCount: Int
        get() = sessionManager.pendingRequests().size

}
