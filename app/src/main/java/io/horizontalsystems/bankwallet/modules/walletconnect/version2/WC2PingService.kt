package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class WC2PingService {

    private val stateSubject = PublishSubject.create<WC2PingServiceState>()
    val stateObservable: Flowable<WC2PingServiceState>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    var state: WC2PingServiceState = WC2PingServiceState.Disconnected()
        private set(value) {
            field = value

            stateSubject.onNext(value)
        }

    fun ping(topic: String) {
        state = WC2PingServiceState.Connecting
        val ping = Sign.Params.Ping(topic)

        SignClient.ping(ping, object : Sign.Listeners.SessionPing {
            override fun onSuccess(pingSuccess: Sign.Model.Ping.Success) {
                state = WC2PingServiceState.Connected
            }

            override fun onError(pingError: Sign.Model.Ping.Error) {
                state = WC2PingServiceState.Disconnected(pingError.error)
            }
        })
    }

    fun receiveResponse() {
        state = WC2PingServiceState.Connected
    }

    fun disconnect() {
        state = WC2PingServiceState.Disconnected()
    }

}
