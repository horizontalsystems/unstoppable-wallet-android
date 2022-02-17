package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import android.util.Log
import com.walletconnect.walletconnectv2.client.WalletConnect
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WalletConnectSessionModule.PeerMetaItem
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2PingService
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WC2SessionService(
    private val service: WC2Service,
    private val pingService: WC2PingService,
    private val connectivityManager: ConnectivityManager,
    private val existingSession: WalletConnect.Model.SettledSession?
) {

    sealed class State {
        object Idle : State()
        class Invalid(val error: Throwable) : State()
        object WaitingForApproveSession : State()
        object Ready : State()
        object Killed : State()
    }

    var proposal: WalletConnect.Model.SessionProposal? = null
        private set

    var session: WalletConnect.Model.SettledSession? = existingSession
        private set

    private val stateSubject = PublishSubject.create<State>()
    val stateObservable: Flowable<State>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    var state: State = State.Idle
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    val appMetaItem: PeerMetaItem?
        get() {
            session?.let {
                return PeerMetaItem(
                    it.topic,
                    it.peerAppMetaData?.url ?: "",
                    it.peerAppMetaData?.description ?: "",
                    it.peerAppMetaData?.icons?.last()
                )
            }
            proposal?.let {
                return PeerMetaItem(
                    it.name,
                    it.url,
                    it.description,
                    it.icons.lastOrNull()?.toString()
                )
            }
            return null
        }

    val connectionState by pingService::state
    val connectionStateObservable by pingService::stateObservable

    private val disposables = CompositeDisposable()

    fun start() {
        existingSession?.let {
            state = State.Ready
            pingService.ping(it.topic)
        }

        connectivityManager.networkAvailabilitySignal
            .subscribeOn(Schedulers.io())
            .subscribe {
                if (!connectivityManager.isConnected){
                    pingService.disconnect()
                } else {
                    session?.let {
                        pingService.ping(it.topic)
                    }
                }
            }
            .let {
                disposables.add(it)
            }

        service.eventObservable
            .subscribe { event ->
                when (event) {
                    is WC2Service.Event.WaitingForApproveSession -> {
                        proposal = event.proposal
                        state = State.WaitingForApproveSession
                        pingService.receiveResponse()
                    }
                    is WC2Service.Event.SessionDeleted -> {
                        session?.topic?.let { topic ->
                            if (topic == event.deletedSession.topic) {
                                state = State.Killed
                                pingService.disconnect()
                            }
                        }
                    }
                    is WC2Service.Event.SessionSettled -> {
                        Log.e("TAG", "start SessionSettled: " )
                        session = event.session
                        state = State.Ready
                        pingService.receiveResponse()
                    }
                }
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    fun stop() {
        service.stop()
        disposables.clear()
    }

    fun reject() {
        proposal?.let {
            service.reject(it)
            pingService.disconnect()
            state = State.Killed
        }
    }

    fun approve() {
        proposal?.let {
            service.approve(it)
        }
    }

    fun disconnect() {
        val sessionNonNull = session ?: return

        service.disconnect(sessionNonNull.topic)
        pingService.disconnect()
        state = State.Killed
    }

    fun reconnect() {
        session?.let {
            pingService.ping(it.topic)
        }
    }
}
