package io.horizontalsystems.walletkit.modules.main

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Tells the main screen when the active wallet syncs through a server in the local network, which
 * the system blocks until the user allows it. Without this the wallet of a user who set such a
 * server up earlier would just stop syncing, with nothing leading them to the network settings
 * screen that asks.
 */
class LocalNetworkAccessViewModel(
    private val walletManager: WalletManager,
    private val zcashEndpointManager: ZcashLightWalletEndpointManager,
    private val connectivityManager: ConnectivityManager,
) : ViewModelUiState<LocalNetworkAccessViewModel.UiState>() {

    private var accessNeeded = false
    private var updateJob: Job? = null

    override fun createState() = UiState(accessNeeded = accessNeeded)

    // A name may be local on one network and public on another, so the answer is looked up again
    // whenever the device moves to another default network. That covers a handover with no loss
    // of connection, which a connected/disconnected signal would not report.
    private val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            viewModelScope.launch { update() }
        }
    }

    init {
        viewModelScope.launch {
            walletManager.activeWalletsUpdatedFlow.collect { update() }
        }
        viewModelScope.launch {
            zcashEndpointManager.currentEndpointUpdatedFlow.collect { update() }
        }
        connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
        update()
    }

    override fun onCleared() {
        connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
    }

    /**
     * The screen asks again each time the app comes to the foreground: the network may have
     * changed while it was away.
     */
    fun refresh() = update()

    private fun update() {
        updateJob?.cancel()
        // Not kept while the lookup runs: on another network the answer may have changed, and a
        // stale "needed" would spend the one dialog of this app run on an endpoint that is public.
        accessNeeded = false
        emitState()
        updateJob = viewModelScope.launch {
            val hasZcashWallet = walletManager.activeWallets.any { it.token.blockchainType == BlockchainType.Zcash }
            accessNeeded = hasZcashWallet && zcashEndpointManager.currentEndpointIsLocal()
            emitState()
        }
    }

    // The synchronizer was started while the system was dropping its connections and may have
    // stopped for good, so the wallet is rebuilt on the same endpoint. Only if that endpoint is
    // still a local one: the user may have moved to a public server before granting access.
    fun onAccessGranted() {
        viewModelScope.launch {
            zcashEndpointManager.retryCurrentEndpointIfLocal()
        }
    }

    data class UiState(val accessNeeded: Boolean)

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LocalNetworkAccessViewModel(
                App.walletManager,
                App.zcashEndpointManager,
                App.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
            ) as T
        }
    }
}
