package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

sealed class WC2SessionServiceState {
    object Idle : WC2SessionServiceState()
    class Invalid(val error: Throwable) : WC2SessionServiceState()
    object WaitingForApproveSession : WC2SessionServiceState()
    object Ready : WC2SessionServiceState()
    object Killed : WC2SessionServiceState()
}