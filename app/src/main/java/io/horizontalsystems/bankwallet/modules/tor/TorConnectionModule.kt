package io.horizontalsystems.bankwallet.modules.tor

object TorConnectionModule {

    data class TorViewState(
        val stateText: Int,
        val showRetryButton: Boolean,
        val torIsActive: Boolean,
        val showNetworkConnectionError: Boolean,
    )

}
