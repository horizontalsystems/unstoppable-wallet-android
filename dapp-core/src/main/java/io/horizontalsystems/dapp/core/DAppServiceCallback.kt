package io.horizontalsystems.dapp.core

interface DAppServiceCallback {
    fun onConnectionStateChange(isAvailable: Boolean)
    fun onError(throwable: Throwable)
    fun onSessionDelete(topic: String)
    fun onSessionProposal(proposal: HSDAppProposal)
    fun onSessionRequest(request: HSDAppRequest)
    fun onSessionSettled(session: HSDAppSession)
    fun onSessionSettleError(errorMessage: String)
    fun onSessionUpdate()
    fun onPairingDelete(topic: String)
}
