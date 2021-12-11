package io.horizontalsystems.bankwallet.modules.walletconnect.list

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.WalletConnectSession
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSessionKillManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSessionManager
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class WalletConnectListService(private val sessionManager: WalletConnectSessionManager) {

    private var sessionKillManager: WalletConnectSessionKillManager? = null
    private val sessionKillDisposable = CompositeDisposable()

    val items: List<Item>
        get() = getItems(sessionManager.sessions)

    val itemsObservable: Flowable<List<Item>>
        get() = sessionManager.sessionsObservable.map { sessions ->
            getItems(sessions)
        }

    val sessionKillingStateObservable = PublishSubject.create<WalletConnectSessionKillManager.State>()

    fun kill(session: WalletConnectSession) {
        sessionKillingStateObservable.onNext(WalletConnectSessionKillManager.State.Processing)

        val sessionKillManager = WalletConnectSessionKillManager(session)

        sessionKillManager.stateObservable
            .subscribeIO {
                onUpdateSessionKillManager(it)
            }.let { sessionKillDisposable.add(it) }

        sessionKillManager.kill()
        this.sessionKillManager = sessionKillManager
    }

    private fun onUpdateSessionKillManager(state: WalletConnectSessionKillManager.State) {
        when (state) {
            is WalletConnectSessionKillManager.State.Failed -> {
                clearSessionKillManager()
            }
            WalletConnectSessionKillManager.State.Killed -> {
                sessionKillManager?.peerId?.let { sessionManager.deleteSession(it) }

                clearSessionKillManager()
            }
        }

        sessionKillingStateObservable.onNext(state)
    }

    private fun clearSessionKillManager() {
        sessionKillManager = null
        sessionKillDisposable.clear()
    }

    private fun getItems(sessions: List<WalletConnectSession>): List<Item> {
        return Chain.values().mapNotNull { chain ->
            val filteredSessions = sessions.filter { it.chainId == chain.value }

            when {
                filteredSessions.isNotEmpty() -> Item(chain, filteredSessions)
                else -> null
            }
        }
    }

    enum class Chain(val value: Int) {
        Ethereum(1),
        BinanceSmartChain(56),
        Ropsten(3),
        Rinkeby(4),
        Kovan(42),
        Goerli(5),
    }

    data class Item(
        val chain: Chain,
        val sessions: List<WalletConnectSession>
    )

}
