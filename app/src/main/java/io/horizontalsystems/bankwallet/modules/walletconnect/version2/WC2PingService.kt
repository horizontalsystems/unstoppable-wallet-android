package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import com.walletconnect.walletconnectv2.client.WalletConnect
import com.walletconnect.walletconnectv2.client.WalletConnectClient
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class WC2PingService {
    sealed class State {
        object Connecting : State()
        object Connected : State()
        class Disconnected(val error: Throwable = Error("Disconnected")) : State()
    }

    private val stateSubject = PublishSubject.create<State>()
    val stateObservable: Flowable<State>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    var state: State = State.Disconnected()
        private set(value) {
            field = value

            stateSubject.onNext(value)
        }

    fun ping(topic: String) {
        state = State.Connecting
        val ping = WalletConnect.Params.Ping(topic)

        WalletConnectClient.ping(ping, object : WalletConnect.Listeners.SessionPing {
            override fun onSuccess(topic: String) {
                state = State.Connected
            }

            override fun onError(error: Throwable) {
                state = State.Disconnected(error)
            }
        })
    }

    fun receiveResponse() {
        state = State.Connected
    }

    fun disconnect() {
        state = State.Disconnected()
    }

}
