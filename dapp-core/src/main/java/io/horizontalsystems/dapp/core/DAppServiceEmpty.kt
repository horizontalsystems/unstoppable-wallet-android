package io.horizontalsystems.dapp.core

class DAppServiceEmpty : DAppService {
    override fun initialize(params: DAppInitParams, callback: DAppServiceCallback) = Unit
    override fun getPairings() = emptyList<HSDAppPairing>()
    override fun disconnectPairing(topic: String, onError: (Throwable) -> Unit) = Unit
    override fun disconnectAllPairings(onError: (Throwable) -> Unit) = Unit
    override fun getActiveSessions() = emptyList<HSDAppSession>()
    override fun disconnectSession(topic: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) = Unit
    override fun getPendingRequests(topic: String) = emptyList<HSDAppRequest>()
    override fun respondRequest(topic: String, requestId: Long, result: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) = Unit
    override fun rejectRequest(topic: String, requestId: Long, onSuccess: () -> Unit, onError: (Throwable) -> Unit) = Unit
    override fun getSessionProposals() = emptyList<HSDAppProposal>()
    override fun generateApprovedNamespaces(proposerPublicKey: String, supportedNamespaces: Map<String, HSDAppNamespaceSession>) = emptyMap<String, HSDAppNamespaceSession>()
    override fun approveSession(proposerPublicKey: String, namespaces: Map<String, HSDAppNamespaceSession>, onSuccess: () -> Unit, onError: (Throwable) -> Unit) = Unit
    override fun rejectSession(proposerPublicKey: String, reason: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) = Unit
    override fun pair(uri: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) = Unit
}
