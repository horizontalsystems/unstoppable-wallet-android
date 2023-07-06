package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.sign.client.Sign
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionServiceState.Invalid
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionServiceState.Killed
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionServiceState.Ready
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionServiceState.WaitingForApproveSession
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Manager
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Parser
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2RequestViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager.RequestDataError.NoSuitableAccount
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager.RequestDataError.NoSuitableEvmKit
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager.RequestDataError.RequestNotFoundError
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager.RequestDataError.UnsupportedChainId
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class WC2SessionViewModel(
    private val wc2service: WC2Service,
    private val wcManager: WC2Manager,
    private val sessionManager: WC2SessionManager,
    private val accountManager: IAccountManager,
    private val connectivityManager: ConnectivityManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val topic: String?,
) : ViewModel() {

    private val TAG = "WC2SessionViewModel"

    val closeLiveEvent = SingleLiveEvent<Unit>()
    val showErrorLiveEvent = SingleLiveEvent<Unit>()

    private val disposables = CompositeDisposable()

    private var peerMeta: WCSessionModule.PeerMetaItem? = null
    private var closeEnabled = false
    private var connecting = false
    private var buttonStates: WCSessionButtonStates? = null
    private var hint: Int? = null
    private var showError: String? = null
    private var blockchains = listOf<WC2SessionModule.BlockchainViewItem>()
    private var status: WCSessionViewModel.Status? = null
    private var pendingRequests = listOf<WC2RequestViewItem>()

    var uiState by mutableStateOf(WC2SessionUiState(
        peerMeta = peerMeta,
        closeEnabled = closeEnabled,
        connecting = connecting,
        buttonStates = buttonStates,
        hint = hint,
        showError = showError,
        blockchains = blockchains,
        status = status,
        pendingRequests = pendingRequests,
    ))
        private set

    private var sessionServiceState: WC2SessionServiceState = WC2SessionServiceState.Idle
        set(value) {
            field = value

            sync(state = value, connection = wc2service.connectionAvailableStateFlow.value)
        }

    private var proposal: Sign.Model.SessionProposal? = null
    private var session: Sign.Model.Session? = null

    private var wcBlockchains = listOf<WCBlockchain>()

    init {
        viewModelScope.launch {
            wc2service.connectionAvailableStateFlow
                .collect {
                    sync(state = sessionServiceState, connection = it)
                }
        }

        viewModelScope.launch {
            wc2service.pendingRequestUpdatedObservable.asFlow()
                .collect {
                    refreshPendingRequests()
                    emitState()
                }
        }

        val topic1 = topic
        if (topic1 != null) {
            val existingSession =
                sessionManager.sessions.firstOrNull { it.topic == topic1 }
            if (existingSession != null) {
                peerMeta = existingSession.metaData?.let {
                    WCSessionModule.PeerMetaItem(
                        it.name,
                        it.url,
                        it.description,
                        it.icons.lastOrNull()?.toString(),
                        accountManager.activeAccount?.name,
                    )
                }

                session = existingSession
                refreshPendingRequests()
                wcBlockchains = getBlockchainsBySession(existingSession)
                sessionServiceState = WC2SessionServiceState.Ready
            }
        } else {
            wc2service.getNextSessionProposal()?.let { sessionProposal ->
                peerMeta = WCSessionModule.PeerMetaItem(
                    sessionProposal.name,
                    sessionProposal.url,
                    sessionProposal.description,
                    sessionProposal.icons.lastOrNull()?.toString(),
                    accountManager.activeAccount?.name,
                )

                proposal = sessionProposal

                try {
                    wcBlockchains = getBlockchainsByProposal(sessionProposal)

                    sessionServiceState = WC2SessionServiceState.WaitingForApproveSession
                } catch (e: WC2SessionManager.RequestDataError) {
                    sessionServiceState = Invalid(e)
                }
            }
        }

        wc2service.eventObservable
            .subscribe { event ->
                when (event) {
                    is WC2Service.Event.WaitingForApproveSession -> {
                    }
                    is WC2Service.Event.SessionDeleted -> {
                        val deletedSession = event.deletedSession
                        if (deletedSession is Sign.Model.DeletedSession.Success) {
                            session?.topic?.let { topic ->
                                if (topic == deletedSession.topic) {
                                    sessionServiceState = Killed
                                }
                            }
                        }
                    }
                    is WC2Service.Event.SessionSettled -> {
                        val session = event.session
                        peerMeta = session.metaData?.let {
                            WCSessionModule.PeerMetaItem(
                                it.name,
                                it.url,
                                it.description,
                                it.icons.lastOrNull()?.toString(),
                                accountManager.activeAccount?.name,
                            )
                        }

                        this.session = session
                        wcBlockchains = getBlockchainsBySession(session)
                        sessionServiceState = WC2SessionServiceState.Ready
                    }
                    is WC2Service.Event.Error -> {
                        sessionServiceState = Invalid(event.error)
                    }
                    WC2Service.Event.Default -> {
                    }
                }
            }
            .let {
                disposables.add(it)
            }
    }

    private fun refreshPendingRequests() {
        pendingRequests = session?.let { existingSession ->
            wc2service.pendingRequests(existingSession.topic).map {
                WC2RequestViewItem(
                    requestId = it.requestId,
                    title = title(it.method),
                    subtitle = it.chainId?.let {
                        WC2Parser.getAccountData(it)
                    }?.chain?.name ?: "",
                )
            }
        } ?: listOf()
    }

    private fun title(method: String?): String = when (method) {
        "personal_sign" -> "Personal Sign Request"
        "eth_sign" -> "Standard Sign Request"
        "eth_signTypedData" -> "Typed Sign Request"
        "eth_sendTransaction" -> "Approve Transaction"
        else -> "Unsupported"
    }
    override fun onCleared() {
        disposables.clear()
    }

    private fun sync(
        state: WC2SessionServiceState,
        connection: Boolean?
    ) {
        val allowedBlockchains = wcBlockchains.sortedBy { it.chainId }

        if (state == Killed) {
            closeLiveEvent.postValue(Unit)
            return
        }

        blockchains = getBlockchainViewItems(allowedBlockchains)
        connecting = connection == null
        closeEnabled = state == Ready
        status = getStatus(connection)
        hint = getHint(connection, state)

        setButtons(state, connection)
        setError(state)

        emitState()
    }

    private fun emitState() {
        uiState = WC2SessionUiState(
            peerMeta = peerMeta,
            closeEnabled = closeEnabled,
            connecting = connecting,
            buttonStates = buttonStates,
            hint = hint,
            showError = showError,
            blockchains = blockchains,
            status = status,
            pendingRequests = pendingRequests,
        )
    }

    fun cancel() {
        val proposal = proposal ?: return

        if (!connectivityManager.isConnected) {
            showErrorLiveEvent.postValue(Unit)
            return
        }

        wc2service.reject(proposal)
        sessionServiceState = Killed
    }

    fun connect() {
        val proposal = proposal ?: return

        if (!connectivityManager.isConnected) {
            showErrorLiveEvent.postValue(Unit)
            return
        }

        if (accountManager.activeAccount == null) {
            sessionServiceState = Invalid(NoSuitableAccount)
            return
        }

        wc2service.approve(proposal, wcBlockchains)
    }

    fun disconnect() {
        if (!connectivityManager.isConnected) {
            showErrorLiveEvent.postValue(Unit)
            return
        }

        val sessionNonNull = session ?: return

        sessionServiceState = Killed
        wc2service.disconnect(sessionNonNull.topic)
    }

    private fun getBlockchainViewItems(blockchains: List<WCBlockchain>) = blockchains.map {
        WC2SessionModule.BlockchainViewItem(
            it.chainId,
            it.name,
            it.address.shorten(),
        )
    }

    private fun getStatus(connectionState: Boolean?): WCSessionViewModel.Status? {
        return when (connectionState) {
            null -> WCSessionViewModel.Status.CONNECTING
            true -> WCSessionViewModel.Status.ONLINE
            false -> WCSessionViewModel.Status.OFFLINE
        }
    }

    private fun setButtons(
        state: WC2SessionServiceState,
        connection: Boolean?
    ) {
        buttonStates = WCSessionButtonStates(
            connect = getConnectButtonState(state, connection),
            disconnect = getDisconnectButtonState(state, connection),
            cancel = getCancelButtonState(state),
            remove = getRemoveButtonState(state, connection),
        )
    }

    private fun getCancelButtonState(state: WC2SessionServiceState): WCButtonState {
        return if (state != Ready) {
            WCButtonState.Enabled
        } else {
            WCButtonState.Hidden
        }
    }

    private fun getConnectButtonState(
        state: WC2SessionServiceState,
        connectionState: Boolean?
    ): WCButtonState {
        return when {
            state == WaitingForApproveSession && connectionState == true -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getDisconnectButtonState(
        state: WC2SessionServiceState,
        connectionState: Boolean?
    ): WCButtonState {
        return when {
            state == Ready && connectionState == true -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getRemoveButtonState(
        state: WC2SessionServiceState,
        connectionState: Boolean?
    ): WCButtonState {
        return when {
            state is Invalid -> WCButtonState.Hidden
            connectionState == false && state is Ready -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun setError(
        state: WC2SessionServiceState
    ) {
        val error: String? = when (state) {
            is Invalid -> state.error.message ?: state.error::class.java.simpleName
            else -> null
        }

        showError = error
    }

    private fun getHint(connection: Boolean?, state: WC2SessionServiceState): Int? {
        when {
            connection == false
                && (state == WaitingForApproveSession || state is Ready) -> {
                return R.string.WalletConnect_Reconnect_Hint
            }
            connection == null -> return null
            state is Invalid -> return getErrorMessage(state.error)
            state == WaitingForApproveSession -> R.string.WalletConnect_Approve_Hint
        }
        return null
    }

    private fun getErrorMessage(error: Throwable): Int? {
        return when (error) {
            is UnsupportedChainId -> R.string.WalletConnect_Error_UnsupportedChainId
            is NoSuitableAccount -> R.string.WalletConnect_Error_NoSuitableAccount
            is NoSuitableEvmKit -> R.string.WalletConnect_Error_NoSuitableEvmKit
            is RequestNotFoundError -> R.string.WalletConnect_Error_RequestNotFoundError
            else -> null
        }
    }


//  session service

    private fun getBlockchainsByProposal(proposal: Sign.Model.SessionProposal): List<WCBlockchain> {
        val account = accountManager.activeAccount ?: throw NoSuitableAccount
        val chains = proposal.requiredNamespaces.values.mapNotNull { it.chains }.flatten()
        val blockchains = getBlockchains(chains, account)
        if (blockchains.size < chains.size) {
            throw UnsupportedChainId
        }

        val optionalChains = proposal.optionalNamespaces.values.mapNotNull { it.chains }.flatten()
        val optionalBlockchains = getBlockchains(optionalChains, account)

        return (blockchains + optionalBlockchains).distinct()
    }

    private fun getBlockchainsBySession(session: Sign.Model.Session): List<WCBlockchain> {
        val account = accountManager.activeAccount ?: return emptyList()
        val accounts = session.namespaces.map { it.value.accounts }.flatten()
        return getBlockchains(accounts, account)
    }

    private fun getBlockchains(accounts: List<String>, account: Account): List<WCBlockchain> {
        val sessionAccountData = accounts.mapNotNull { WC2Parser.getAccountData(it) }
        return sessionAccountData.mapNotNull { data ->
            val address = wcManager.getEvmAddress(account, data.chain).eip55
            evmBlockchainManager.getBlockchain(data.chain.id)?.let { supportedBlockchain ->
                WCBlockchain(data.chain.id, supportedBlockchain.name, address)
            }
        }
    }

}

data class WC2SessionUiState(
    val peerMeta: WCSessionModule.PeerMetaItem?,
    val closeEnabled: Boolean,
    val connecting: Boolean,
    val buttonStates: WCSessionButtonStates?,
    val hint: Int?,
    val showError: String?,
    val blockchains: List<WC2SessionModule.BlockchainViewItem>,
    val status: WCSessionViewModel.Status?,
    val pendingRequests: List<WC2RequestViewItem>
)
