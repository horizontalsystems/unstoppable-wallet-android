package io.horizontalsystems.bankwallet.modules.walletconnect.list.v2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.walletconnect.walletconnectv2.client.WalletConnect
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule.Section
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Parser
import io.reactivex.disposables.CompositeDisposable

class WC2ListViewModel(
    private val service: WC2ListService
) : ViewModel() {

    private val disposables = CompositeDisposable()
    var sectionItem by mutableStateOf<Section?>(null)
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

    private fun sync(sessions: List<WalletConnect.Model.SettledSession>) {
        if (sessions.isEmpty() && service.pendingRequestsCount == 0) {
            sectionItem = null
            return
        }

        val sessionItems = sessions.map { session ->
            WalletConnectListModule.SessionViewItem(
                sessionId = session.topic,
                title = session.peerAppMetaData?.name ?: "",
                subtitle = getSubtitle(session.permissions.blockchain.chains),
                url = session.peerAppMetaData?.url ?: "",
                imageUrl = session.peerAppMetaData?.icons?.lastOrNull(),
            )
        }

        setSectionViewItem(sessionItems, service.pendingRequestsCount)
    }

    private fun getSubtitle(chains: List<String>): String {
        val chainNames = chains.mapNotNull { WC2Parser.getChainName(it) }
        return chainNames.joinToString(", ")
    }

    override fun onCleared() {
        disposables.clear()
    }

}
