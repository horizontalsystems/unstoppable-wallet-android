package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
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
        val ping = Sign.Params.Ping(topic)

        SignClient.ping(ping, object : Sign.Listeners.SessionPing {
            override fun onSuccess(pingSuccess: Sign.Model.Ping.Success) {
                state = State.Connected
            }

            override fun onError(pingError: Sign.Model.Ping.Error) {
                state = State.Disconnected(pingError.error)
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
