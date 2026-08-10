package io.horizontalsystems.walletkit.modules.walletconnect

import io.horizontalsystems.dapp.core.HSDAppRequest

/** Handles wallet_* WalletConnect requests silently, returning true when consumed. */
interface IWCWalletRequestHandler {
    fun handle(request: HSDAppRequest): Boolean
}
