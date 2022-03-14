package io.horizontalsystems.bankwallet.modules.walletconnect.list.v1

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.entity.WalletConnectSession
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WCChain
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

    fun kill(sessionId: String) {
        sessionKillingStateObservable.onNext(WC1SessionKillManager.State.Processing)

        items.forEach { item ->
            item.sessions.forEach { session ->
                if (session.remotePeerId == sessionId){
                    val sessionKillManager = WC1SessionKillManager(session)
                    sessionKillManager.stateObservable
                        .subscribeIO {
                            onUpdateSessionKillManager(it)
                        }.let { sessionKillDisposable.add(it) }

                    sessionKillManager.kill()
                    this.sessionKillManager = sessionKillManager
                }
            }
        }
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
        return WCChain.values().mapNotNull { chain ->
            val filteredSessions = sessions.filter { it.chainId == chain.id }

            when {
                filteredSessions.isNotEmpty() -> Item(chain, filteredSessions)
                else -> null
            }
        }
    }

    data class Item(
        val chain: WCChain,
        val sessions: List<WalletConnectSession>
    )

}
