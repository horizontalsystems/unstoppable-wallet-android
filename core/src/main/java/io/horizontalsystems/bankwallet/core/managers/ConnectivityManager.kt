package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.BackgroundManagerState
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class ConnectivityManager(backgroundManager: BackgroundManager) {

    private val connectivityManager: ConnectivityManager by lazy {
        App.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _networkAvailabilityFlow =
        MutableSharedFlow<Boolean>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val networkAvailabilityFlow = _networkAvailabilityFlow.asSharedFlow()

    var isConnected = getInitialConnectionStatus()
    val networkAvailabilitySignal = PublishSubject.create<Unit>()

    private var callback = ConnectionStatusCallback()
    private var hasValidInternet = false
    private var hasConnection = false

    init {
        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> {
                        willEnterForeground()
                    }
                    BackgroundManagerState.EnterBackground -> {
                        didEnterBackground()
                    }
                }
            }
        }
    }

    private fun willEnterForeground() {
        setInitialValues()
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            //was not registered, or already unregistered
        }
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    private fun didEnterBackground() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            //already unregistered
        }
    }

    private fun setInitialValues() {
        hasConnection = false
        hasValidInternet = false
        isConnected = getInitialConnectionStatus()
        networkAvailabilitySignal.onNext(Unit)
        _networkAvailabilityFlow.tryEmit(isConnected)
    }

    private fun getInitialConnectionStatus(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false

        hasConnection = true
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        hasValidInternet = capabilities?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false

        return hasValidInternet
    }

    inner class ConnectionStatusCallback : ConnectivityManager.NetworkCallback() {

        private val activeNetworks: MutableList<Network> = mutableListOf()

        override fun onLost(network: Network) {
            super.onLost(network)
            activeNetworks.removeAll { activeNetwork -> activeNetwork == network }
            hasConnection = activeNetworks.isNotEmpty()
            updatedConnectionState()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            hasValidInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            updatedConnectionState()
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (activeNetworks.none { activeNetwork -> activeNetwork == network }) {
                activeNetworks.add(network)
            }
            hasConnection = activeNetworks.isNotEmpty()
            updatedConnectionState()
        }

    }

    private fun updatedConnectionState() {
        val oldValue = isConnected
        isConnected = hasConnection && hasValidInternet
        if (oldValue != isConnected) {
            networkAvailabilitySignal.onNext(Unit)
            _networkAvailabilityFlow.tryEmit(isConnected)
        }
    }

}
