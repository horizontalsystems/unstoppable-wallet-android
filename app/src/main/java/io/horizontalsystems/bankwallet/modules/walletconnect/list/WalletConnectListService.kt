package io.horizontalsystems.bankwallet.modules.walletconnect.list

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.WalletConnectSession
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SessionKillManager
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SessionManager
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class WalletConnectListService(private val sessionManager: WC1SessionManager) {

    private var sessionKillManager: WC1SessionKillManager? = null
    private val sessionKillDisposable = CompositeDisposable()

    val items: List<Item>
        get() = getItems(sessionManager.sessions)

    val itemsObservable: Flowable<List<Item>>
        get() = sessionManager.sessionsObservable.map { sessions ->
            getItems(sessions)
        }

    val sessionKillingStateObservable = PublishSubject.create<WC1SessionKillManager.State>()

    fun kill(session: WalletConnectSession) {
        sessionKillingStateObservable.onNext(WC1SessionKillManager.State.Processing)

        val sessionKillManager = WC1SessionKillManager(session)

        sessionKillManager.stateObservable
            .subscribeIO {
                onUpdateSessionKillManager(it)
            }.let { sessionKillDisposable.add(it) }

        sessionKillManager.kill()
        this.sessionKillManager = sessionKillManager
    }

    private fun onUpdateSessionKillManager(state: WC1SessionKillManager.State) {
        when (state) {
            is WC1SessionKillManager.State.Failed -> {
                clearSessionKillManager()
            }
            WC1SessionKillManager.State.Killed -> {
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
