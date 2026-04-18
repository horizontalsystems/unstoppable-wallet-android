package io.horizontalsystems.dapp.core

import android.app.Application

data class DAppInitParams(
    val application: Application,
    val projectId: String,
    val relayServerUrl: String,
    val appName: String,
    val appUrl: String,
    val appIcon: String,
)

interface DAppService {
    fun initialize(params: DAppInitParams, callback: DAppServiceCallback)

    // Pairings
    fun getPairings(): List<HSDAppPairing>
    fun disconnectPairing(topic: String, onError: (Throwable) -> Unit = {})
    fun disconnectAllPairings(onError: (Throwable) -> Unit = {})

    // Sessions
    fun getActiveSessions(): List<HSDAppSession>
    fun disconnectSession(topic: String, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {})

    // Requests
    fun getPendingRequests(topic: String): List<HSDAppRequest>
    fun respondRequest(topic: String, requestId: Long, result: String, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {})
    fun rejectRequest(topic: String, requestId: Long, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {})

    // Session proposals
    fun getSessionProposals(): List<HSDAppProposal>
    fun generateApprovedNamespaces(proposerPublicKey: String, supportedNamespaces: Map<String, HSDAppNamespaceSession>): Map<String, HSDAppNamespaceSession>
    fun approveSession(proposerPublicKey: String, namespaces: Map<String, HSDAppNamespaceSession>, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {})
    fun rejectSession(proposerPublicKey: String, reason: String = "Reject Session", onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {})

    // Pairing
    fun pair(uri: String, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {})
}
