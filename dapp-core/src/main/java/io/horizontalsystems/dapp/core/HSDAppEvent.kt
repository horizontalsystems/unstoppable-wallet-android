package io.horizontalsystems.dapp.core

sealed class HSDAppEvent {
    data class ConnectionState(val isAvailable: Boolean) : HSDAppEvent()
    data class Error(val throwable: Throwable) : HSDAppEvent()
    data class SessionDelete(val topic: String) : HSDAppEvent()
    data class SessionSettled(val session: HSDAppSession) : HSDAppEvent()
    data class SessionSettleError(val errorMessage: String) : HSDAppEvent()
    data class SessionProposal(val proposal: HSDAppProposal) : HSDAppEvent()
    data class SessionRequest(val request: HSDAppRequest) : HSDAppEvent()
    object SessionUpdate : HSDAppEvent()
    data class PairingDelete(val topic: String) : HSDAppEvent()
}
