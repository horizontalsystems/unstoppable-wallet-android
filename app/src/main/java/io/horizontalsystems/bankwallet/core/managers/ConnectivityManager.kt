package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.BackgroundManager
import io.reactivex.subjects.PublishSubject


class ConnectivityManager(backgroundManager: BackgroundManager): BackgroundManager.Listener {

    private val connectivityManager: ConnectivityManager by lazy {
        App.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    var isConnected = getInitialConnectionStatus()
    val networkAvailabilitySignal = PublishSubject.create<Unit>()

    private var callback = ConnectionStatusCallback()
    private var hasValidInternet = false
    private var hasConnection = false

    init {
        backgroundManager.registerListener(this)
    }

    override fun willEnterForeground() {
        super.willEnterForeground()
        setInitialValues()
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            //was not registered, or already unregistered
        }
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    override fun didEnterBackground() {
        super.didEnterBackground()
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
        }
    }

}
