package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.android.CoreClient
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCRequestViewItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletConnectListViewModel(
    private val wcSessionManager: WCSessionManager,
    private val wcManager: WCManager,
) : ViewModel() {
    enum class ConnectionResult {
        Success, Error
    }

    private var pendingRequests = listOf<WCRequestViewItem>()
    private var pendingRequestCountMap = mutableMapOf<String, Int>()
    private var pairingsNumber = 0
    private var showError: String? = null
    private var _refreshFlow: MutableSharedFlow<Unit> =
        MutableSharedFlow(replay = 0, extraBufferCapacity = 1, BufferOverflow.DROP_OLDEST)
    private var refreshFlow: SharedFlow<Unit> = _refreshFlow.asSharedFlow()
    private val uiStateFlow = merge(WCDelegate.walletEvents, refreshFlow).map {
        getUiState()
    }

    val uiState: StateFlow<WalletConnectListUiState> =
        uiStateFlow.stateIn(viewModelScope, SharingStarted.Eagerly, getUiState())

    var connectionResult by mutableStateOf<ConnectionResult?>(null)
        private set

    init {
        syncPairingCount()
        syncPendingRequestsCountMap()

        viewModelScope.launch {
            WCDelegate.pendingRequestEvents.collect {
                syncPendingRequestsCountMap()
            }
        }
        viewModelScope.launch {
            WCDelegate.pairingEvents.collect {
                syncPairingCount()
            }
        }

        viewModelScope.launch {
            WCDelegate.walletEvents.collect {
                syncPairingCount()
            }
        }
    }

    fun refreshList() {
        _refreshFlow.tryEmit(Unit)
    }

    fun setRequestToOpen(request: Wallet.Model.SessionRequest) {
        WCDelegate.sessionRequestEvent = request
    }

    fun setConnectionUri(uri: String) {
        if (uri.contains("requestId")) {
            //wc also creates deeplinks for Pending Request
            //we should ignore these deeplinks
            return
        }
        connectionResult = when (WalletConnectListModule.getVersionFromUri(uri)) {
            2 -> {
                Web3Wallet.pair(
                    Wallet.Params.Pair(uri.trim()),
                    onSuccess = {
                        connectionResult = null
                    },
                    onError = {
                        connectionResult = ConnectionResult.Error
                    }
                )
                null
            }

            else -> ConnectionResult.Error
        }
    }

    fun onDelete(topic: String) {
        WCDelegate.deleteSession(
            topic = topic,
            onSuccess = {
                _refreshFlow.tryEmit(Unit)
            },
            onError = {
                showError = it.message
                _refreshFlow.tryEmit(Unit)
            }
        )
    }

    fun onRouteHandled() {
        connectionResult = null
    }

    fun errorShown() {
        showError = null
        _refreshFlow.tryEmit(Unit)
    }

    private fun getUiState(): WalletConnectListUiState {
        return WalletConnectListUiState(
            sessionViewItems = getSessions(wcSessionManager.sessions),
            pairingsNumber = pairingsNumber,
        )
    }

    private fun getSessions(sessions: List<Wallet.Model.Session>): List<WalletConnectListModule.SessionViewItem> {
        val sessionItems = sessions.map { session ->
            WalletConnectListModule.SessionViewItem(
                sessionTopic = session.topic,
                title = session.metaData?.name ?: "",
                subtitle = session.metaData?.url?.let { TextHelper.getCleanedUrl(it) } ?: "",
                url = session.metaData?.url ?: "",
                imageUrl = session.metaData?.icons?.lastOrNull(),
                pendingRequestsCount = pendingRequestCountMap[session.topic] ?: 0,
                requests = getPendingRequestViewItems(session.topic)
            )
        }
        return sessionItems
    }

    private fun syncPairingCount() {
        viewModelScope.launch(Dispatchers.IO) {
            pairingsNumber = getPairingCount()
            _refreshFlow.tryEmit(Unit)
        }
    }

    private fun syncPendingRequestsCountMap() {
        viewModelScope.launch(Dispatchers.IO) {
            wcSessionManager.sessions.forEach { session ->
                val requests = Web3Wallet.getPendingListOfSessionRequests(session.topic)
                pendingRequestCountMap[session.topic] = requests.size
                pendingRequests = getPendingRequestViewItems(session.topic)
            }
            _refreshFlow.tryEmit(Unit)
        }
    }

    private fun getPairingCount(): Int {
        return CoreClient.Pairing.getPairings().size
    }

    private fun getPendingRequestViewItems(topic: String): List<WCRequestViewItem> {
        return Web3Wallet.getPendingListOfSessionRequests(topic).map { request ->
            val methodData = wcManager.getMethodData(request)

            WCRequestViewItem(
                title = methodData?.shortTitle ?: "Unsupported",
                subtitle = methodData?.network ?: "",
                request = request
            )
        }
    }
}

data class WalletConnectListUiState(
    val sessionViewItems: List<WalletConnectListModule.SessionViewItem> = emptyList(),
    val pairingsNumber: Int = 0,
    val showError: String? = null,
)
