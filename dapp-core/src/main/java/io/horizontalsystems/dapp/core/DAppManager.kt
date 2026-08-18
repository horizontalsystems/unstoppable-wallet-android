package io.horizontalsystems.dapp.core

import android.util.Log

object DAppManager {
    private var service: DAppService = DAppServiceEmpty()

    val isAvailable: Boolean
        get() = service !is DAppServiceEmpty

    fun registerService(service: DAppService) {
        DAppManager.service = service
    }

    fun initialize(params: DAppInitParams, callback: DAppServiceCallback) {
        try {
            service.initialize(params, callback)
        } catch (e: Throwable) {
            // The dApp engine can fail to start on some devices (e.g. WalletConnect's
            // CoreClient reports init failure only via callback and setDelegate then
            // throws). Initialize runs in Application.onCreate, so a throw here is a
            // startup crash loop — degrade to no dApp support instead.
            Log.e("DAppManager", "dApp service initialization failed", e)
            service = DAppServiceEmpty()
        }
    }

    fun getPairings(): List<HSDAppPairing> = service.getPairings()

    fun disconnectPairing(topic: String, onError: (Throwable) -> Unit = {}) =
        service.disconnectPairing(topic, onError)

    fun disconnectAllPairings(onError: (Throwable) -> Unit = {}) =
        service.disconnectAllPairings(onError)

    fun getActiveSessions(): List<HSDAppSession> = service.getActiveSessions()

    fun disconnectSession(topic: String, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {}) =
        service.disconnectSession(topic, onSuccess, onError)

    fun getPendingRequests(topic: String): List<HSDAppRequest> = service.getPendingRequests(topic)

    fun respondRequest(topic: String, requestId: Long, result: String, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {}) =
        service.respondRequest(topic, requestId, result, onSuccess, onError)

    fun rejectRequest(topic: String, requestId: Long, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {}) =
        service.rejectRequest(topic, requestId, onSuccess, onError)

    fun getSessionProposals(): List<HSDAppProposal> = service.getSessionProposals()

    fun generateApprovedNamespaces(proposerPublicKey: String, supportedNamespaces: Map<String, HSDAppNamespaceSession>): Map<String, HSDAppNamespaceSession> =
        service.generateApprovedNamespaces(proposerPublicKey, supportedNamespaces)

    fun approveSession(proposerPublicKey: String, namespaces: Map<String, HSDAppNamespaceSession>, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {}) =
        service.approveSession(proposerPublicKey, namespaces, onSuccess, onError)

    fun rejectSession(proposerPublicKey: String, reason: String = "Reject Session", onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {}) =
        service.rejectSession(proposerPublicKey, reason, onSuccess, onError)

    fun pair(uri: String, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {}) =
        service.pair(uri, onSuccess, onError)
}
