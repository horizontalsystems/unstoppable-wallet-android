package io.horizontalsystems.bankwallet.modules.walletconnect

import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.entities.WalletConnectSession
import io.reactivex.subjects.PublishSubject

class WalletConnectSessionKillManager(
    session: WalletConnectSession
) : WalletConnectInteractor.Delegate {

    private val interactor = WalletConnectInteractor(session.session, remotePeerId = session.remotePeerId)

    val stateObservable = PublishSubject.create<State>()
    var state: State = State.NotConnected
        private set(value) {
            field = value
            stateObservable.onNext(value)
        }

    val peerId = session.remotePeerId

    init {
        interactor.delegate = this
    }

    fun kill() {
        if (interactor.state is WalletConnectInteractor.State.Idle) {
            interactor.connect()
        }
    }

    override fun didUpdateState(state: WalletConnectInteractor.State) {
        when (state) {
            WalletConnectInteractor.State.Connected -> {
                interactor.killSession()
            }
            WalletConnectInteractor.State.Idle,
            WalletConnectInteractor.State.Connecting -> {
                this.state = State.Processing
            }
            is WalletConnectInteractor.State.Disconnected -> {
                this.state = State.Killed
            }
        }
    }

    override fun didKillSession() {
        state = State.Killed
    }

    override fun didReceiveError(error: Throwable) {
        state = State.Failed(error)
    }

    sealed class State {
        object NotConnected : State()
        object Processing : State()
        object Killed : State()
        class Failed(val error: Throwable) : State()
    }

}
