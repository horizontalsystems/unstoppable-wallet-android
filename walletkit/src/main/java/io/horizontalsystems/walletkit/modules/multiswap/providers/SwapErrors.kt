package io.horizontalsystems.walletkit.modules.multiswap.providers

sealed class SwapError : Exception() {
    class NoDestinationAddress : SwapError()
}
