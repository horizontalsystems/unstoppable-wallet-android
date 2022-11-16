package io.horizontalsystems.bankwallet.modules.walletconnect.version2

sealed class WC2PingServiceState {
    object Connecting : WC2PingServiceState()
    object Connected : WC2PingServiceState()
    class Disconnected(val error: Throwable = Error("Disconnected")) : WC2PingServiceState()
}