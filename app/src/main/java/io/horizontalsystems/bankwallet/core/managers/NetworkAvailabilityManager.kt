package io.horizontalsystems.bankwallet.core.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import io.horizontalsystems.bankwallet.core.App
import io.reactivex.subjects.PublishSubject

class NetworkAvailabilityManager {

    private val connectivityManager = App.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var isConnected = connectivityManager.activeNetworkInfo?.isConnected ?: false
    val networkAvailabilitySignal = PublishSubject.create<Unit>()

    init {
        App.instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onUpdateStatus()
            }
        }, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
    }

    private fun onUpdateStatus() {
        val newIsConnected = connectivityManager.activeNetworkInfo?.isConnected ?: false

        if (isConnected != newIsConnected) {
            isConnected = newIsConnected
            networkAvailabilitySignal.onNext(Unit)
        }
    }

}
