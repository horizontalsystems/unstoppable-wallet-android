package io.horizontalsystems.bankwallet.modules.walletconnect.list

import io.horizontalsystems.bankwallet.entities.WalletConnectSession
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSessionManager
import io.reactivex.Flowable

class WalletConnectListService(private val sessionManager: WalletConnectSessionManager) {

    val items: List<Item>
        get() = getItems(sessionManager.sessions)

    val itemsObservable: Flowable<List<Item>>
        get() = sessionManager.sessionsObservable.map { sessions ->
            getItems(sessions)
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
