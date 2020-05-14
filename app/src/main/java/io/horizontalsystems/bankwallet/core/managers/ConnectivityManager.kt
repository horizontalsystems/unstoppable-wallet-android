package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.horizontalsystems.bankwallet.core.App
import io.reactivex.subjects.PublishSubject


class ConnectivityManager {

    private val connectivityManager: ConnectivityManager by lazy {
        App.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    var isConnected = false
    val networkAvailabilitySignal = PublishSubject.create<Unit>()

    init {
        listenNetworkViaConnectivityManager()
    }

    private fun onUpdateStatus(stateIsConnected: Boolean) {
        if (isConnected != stateIsConnected) {
            isConnected = stateIsConnected
            networkAvailabilitySignal.onNext(Unit)
        }
    }

    private fun listenNetworkViaConnectivityManager() {
        val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onUpdateStatus(true)
            }

            override fun onLost(network: Network?) {
                onUpdateStatus(false)
            }

        })

    }

}
