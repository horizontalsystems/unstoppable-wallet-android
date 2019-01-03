package io.horizontalsystems.bankwallet.core.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import io.horizontalsystems.bankwallet.core.App
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class NetworkAvailabilityManager {

    val stateSubject: PublishSubject<Boolean> = PublishSubject.create()
    private val connectivityManager = App.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val activeNetworkInfo: NetworkInfo?
        get() = connectivityManager.activeNetworkInfo

    private val isNetworkAvailable: Boolean
        get() = activeNetworkInfo?.isConnected ?: false

    init {
        App.instance.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                stateSubject.onNext(isNetworkAvailable)
                state2.onNext(isNetworkAvailable)
            }
        }, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
    }

    private val state2 = BehaviorSubject.createDefault(isNetworkAvailable)
    val stateObservable: Flowable<Boolean> = state2.toFlowable(BackpressureStrategy.DROP)
}
