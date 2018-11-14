package io.horizontalsystems.bankwallet.core.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import io.horizontalsystems.bankwallet.core.App
import io.reactivex.subjects.PublishSubject

class NetworkAvailabilityManager {

    val stateSubject: PublishSubject<Boolean> = PublishSubject.create()
    private val connectivityManager = App.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val activeNetworkInfo: NetworkInfo?
        get() = connectivityManager.activeNetworkInfo

    val isNetworkAvailable: Boolean
        get() = activeNetworkInfo?.isConnected ?: false

    init {
        App.instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                stateSubject.onNext(isNetworkAvailable)
            }
        }, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
    }
}
