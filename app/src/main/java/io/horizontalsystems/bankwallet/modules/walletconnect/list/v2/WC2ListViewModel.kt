package io.horizontalsystems.bankwallet.modules.walletconnect.list.v2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.walletconnect.sign.client.Sign
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule.Section
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Parser
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service
import io.reactivex.disposables.CompositeDisposable

class WC2ListViewModel(
    private val service: WC2ListService,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val wc2Service: WC2Service
) : ViewModel() {

    private val disposables = CompositeDisposable()
    var sectionItem by mutableStateOf<Section?>(null)
        private set

    var pairingsNumber by mutableStateOf(wc2Service.getPairings().size)
        private set

    init {
        service.sessionsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        service.pendingRequestsObservable
            .subscribeIO { syncPendingRequestsCount(it) }
            .let { disposables.add(it) }

        sync(service.sessions)
    }

    override fun onCleared() {
        disposables.clear()
    }

    private fun syncPendingRequestsCount(count: Int) {
        sectionItem?.let {
            setSectionViewItem(it.sessions, count)
        }
    }

    private fun setSectionViewItem(
        sessions: List<WalletConnectListModule.SessionViewItem>,
        pendingRequestsCount: Int
    ) {
        val count = if (pendingRequestsCount > 0) pendingRequestsCount else null
        sectionItem = Section(WalletConnectListModule.Version.Version2, sessions, count)
    }

    private fun sync(sessions: List<Sign.Model.Session>) {
        if (sessions.isEmpty() && service.pendingRequestsCount == 0) {
            sectionItem = null
            return
        }

        val sessionItems = sessions.map { session ->
            WalletConnectListModule.SessionViewItem(
                sessionId = session.topic,
                title = session.metaData?.name ?: "",
                subtitle = getSubtitle(session.namespaces.values.map { it.accounts }.flatten()),
                url = session.metaData?.url ?: "",
                imageUrl = session.metaData?.icons?.lastOrNull(),
            )
        }

        setSectionViewItem(sessionItems, service.pendingRequestsCount)
    }

    private fun getSubtitle(chains: List<String>): String {
        val chainNames = chains.mapNotNull { chain ->
            WC2Parser.getChainId(chain)?.let { chainId ->
                evmBlockchainManager.getBlockchain(chainId)?.name
            }
        }
        return chainNames.joinToString(", ")
    }

    fun onDelete(sessionId: String) {
        service.delete(sessionId)
    }

}
