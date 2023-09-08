package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.sign.client.Sign
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule.Section
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Parser
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class WalletConnectListViewModel(
    private val wC2SessionManager: WC2SessionManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val wc2Service: WC2Service
) : ViewModel() {
    enum class ConnectionResult {
        Success, Error
    }

    var connectionResult by mutableStateOf<ConnectionResult?>(null)
        private set

    var initialConnectionPrompted = false

    private var v2SectionItem: Section? = null
    private var pairingsNumber = wc2Service.getPairings().size
    private val emptyScreen: Boolean
        get() = v2SectionItem == null && pairingsNumber == 0

    var uiState by mutableStateOf(
        WalletConnectListUiState(
            v2SectionItem = v2SectionItem,
            pairingsNumber = pairingsNumber,
            emptyScreen = emptyScreen,
        )
    )
        private set

    private val disposables = CompositeDisposable()

    init {
        wC2SessionManager.sessionsObservable
            .subscribeIO {
                syncV2(it)
                emitState()
            }
            .let { disposables.add(it) }

        viewModelScope.launch {
            wC2SessionManager.pendingRequestCountFlow
                .collect {
                    syncV2(wC2SessionManager.sessions)
                    emitState()
                }
        }

        syncV2(wC2SessionManager.sessions)
        emitState()
    }

    fun setConnectionUri(uri: String) {
        connectionResult = when (WalletConnectListModule.getVersionFromUri(uri)) {
            2 -> {
                wc2Service.pair(uri)
                null
            }

            else -> ConnectionResult.Error
        }
    }

    private fun emitState() {
        uiState = WalletConnectListUiState(
            v2SectionItem = v2SectionItem,
            pairingsNumber = pairingsNumber,
            emptyScreen = emptyScreen
        )
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun refreshPairingsNumber() {
        pairingsNumber = wc2Service.getPairings().size
        emitState()
    }

    private fun syncV2(sessions: List<Sign.Model.Session>) {
        if (sessions.isEmpty()) {
            v2SectionItem = null
            return
        }

        val sessionItems = sessions.map { session ->
            WalletConnectListModule.SessionViewItem(
                sessionId = session.topic,
                title = session.metaData?.name ?: "",
                subtitle = getSubtitle(session.namespaces.values.map { it.accounts }.flatten()),
                url = session.metaData?.url ?: "",
                imageUrl = session.metaData?.icons?.lastOrNull(),
                pendingRequestsCount = wc2Service.pendingRequests(session.topic).size,
            )
        }

        v2SectionItem = Section(WalletConnectListModule.Version.Version2, sessionItems)
    }

    private fun getSubtitle(chains: List<String>): String {
        val chainNames = chains.mapNotNull { chain ->
            WC2Parser.getChainId(chain)?.let { chainId ->
                evmBlockchainManager.getBlockchain(chainId)?.name
            }
        }
        return chainNames.joinToString(", ")
    }

    fun onDeleteV2(sessionId: String) {
        wC2SessionManager.deleteSession(sessionId)
    }

    fun onHandleRoute() {
        connectionResult = null
    }
}

data class WalletConnectListUiState(
    val v2SectionItem: Section?,
    val pairingsNumber: Int,
    val emptyScreen: Boolean
)
