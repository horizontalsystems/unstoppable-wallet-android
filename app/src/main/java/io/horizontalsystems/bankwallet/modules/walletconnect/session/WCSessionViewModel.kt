package io.horizontalsystems.bankwallet.modules.walletconnect.session

import androidx.lifecycle.viewModelScope
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager.RequestDataError.NoSuitableAccount
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager.RequestDataError.NoSuitableEvmKit
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager.RequestDataError.RequestNotFoundError
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager.RequestDataError.UnsupportedChainId
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionServiceState.Invalid
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionServiceState.Killed
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionServiceState.Ready
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionServiceState.WaitingForApproveSession
import io.horizontalsystems.core.SingleLiveEvent
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class WCSessionViewModel(
    private val sessionManager: WCSessionManager,
    private val connectivityManager: ConnectivityManager,
    private val account: Account?,
    private val topic: String?,
    private val wcManager: WCManager
) : ViewModelUiState<WCSessionUiState>() {

    val closeLiveEvent = SingleLiveEvent<Unit>()
    val showErrorLiveEvent = SingleLiveEvent<String?>()
    val showNoInternetErrorLiveEvent = SingleLiveEvent<Unit>()

    private var peerMeta: PeerMetaItem? = null
    private var closeEnabled = false
    private var connecting = false
    private var buttonStates: WCSessionButtonStates? = null
    private var hint: String? = null
    private var showError: String? = null
    private var status: Status? = null
    private var pendingRequests = listOf<WCRequestViewItem>()

    override fun createState() = WCSessionUiState(
        peerMeta = peerMeta,
        closeEnabled = closeEnabled,
        connecting = connecting,
        buttonStates = buttonStates,
        hint = hint,
        showError = showError,
        status = status,
        pendingRequests = pendingRequests,
    )

    private var sessionServiceState: WCSessionServiceState = WCSessionServiceState.Idle
        set(value) {
            field = value

            sync(state = value, connection = WCDelegate.connectionAvailableEvent.value)
        }

    private var proposal: Wallet.Model.SessionProposal? = null
    private var session: Wallet.Model.Session? = null

    init {
        viewModelScope.launch {
            WCDelegate.connectionAvailableEvent.collect {
                sync(state = sessionServiceState, connection = it)
            }
        }

        viewModelScope.launch {
            WCDelegate.pendingRequestEvents.collect {
                session?.let { existingSession ->
                    pendingRequests = getPendingRequestViewItems(existingSession.topic)
                    emitState()
                }
            }
        }

        viewModelScope.launch {
            WCDelegate.walletEvents.collect { event ->
                when (event) {
                    is Wallet.Model.SessionDelete -> {
                        when (event) {
                            is Wallet.Model.SessionDelete.Success -> {
                                session?.topic?.let { topic ->
                                    if (topic == event.topic) {
                                        sessionServiceState = Killed
                                    }
                                }
                            }

                            is Wallet.Model.SessionDelete.Error -> {
                                sessionServiceState = Invalid(event.error)
                            }
                        }

                        sessionServiceState = Killed
                    }

                    is Wallet.Model.Error -> {
                        sessionServiceState = Invalid(event.throwable)
                    }

                    is Wallet.Model.SettledSessionResponse -> {
                        when (event) {
                            is Wallet.Model.SettledSessionResponse.Result -> {
                                val session = event.session
                                peerMeta = session.metaData?.let {
                                    PeerMetaItem(
                                        it.name,
                                        it.url,
                                        it.description,
                                        it.icons.lastOrNull()?.toString(),
                                        account?.name,
                                    )
                                }

                                this@WCSessionViewModel.session = session
                                sessionServiceState = Ready
                            }

                            is Wallet.Model.SettledSessionResponse.Error -> {
                                sessionServiceState = Invalid(Throwable(event.errorMessage))
                            }
                        }
                    }

                    else -> {

                    }
                }
            }
        }

        viewModelScope.launch {
            WCDelegate.pendingRequestEvents.collect {
                topic?.let {
                    pendingRequests = getPendingRequestViewItems(it)
                    emitState()
                }
            }
        }

        loadSessionProposal(topic)
    }

    private fun loadSessionProposal(topic: String?) {
        if (topic != null) {
            val existingSession = sessionManager.sessions.firstOrNull { it.topic == topic }
            if (existingSession != null) {
                peerMeta = existingSession.metaData?.let {
                    PeerMetaItem(
                        it.name,
                        it.url,
                        it.description,
                        it.icons.lastOrNull()?.toString(),
                        account?.name,
                    )
                }

                session = existingSession
                pendingRequests = getPendingRequestViewItems(topic)
                sessionServiceState = Ready
            }
        } else {
            WCDelegate.sessionProposalEvent?.let { (sessionProposal, _) ->
                peerMeta = PeerMetaItem(
                    sessionProposal.name,
                    sessionProposal.url,
                    sessionProposal.description,
                    sessionProposal.icons.lastOrNull()?.toString(),
                    account?.name,
                )
                proposal = sessionProposal

                sessionServiceState = try {
                    wcManager.validate(sessionProposal.requiredNamespaces)

                    WaitingForApproveSession
                } catch (e: Throwable) {
                    Invalid(e)
                }
            } ?: run {
                sessionServiceState = Invalid(RequestNotFoundError)
            }
        }
    }

    private fun getPendingRequestViewItems(topic: String): List<WCRequestViewItem> {
        return Web3Wallet.getPendingListOfSessionRequests(topic).map { request ->
            val methodData = wcManager.getMethodData(request)

            WCRequestViewItem(
                title = methodData?.title ?: "Unsupported",
                subtitle = methodData?.network ?: "",
                request = request
            )
        }
    }

    private fun sync(
        state: WCSessionServiceState,
        connection: Boolean?
    ) {
        if (state == Killed) {
            closeLiveEvent.postValue(Unit)
            return
        }

        connecting = connection == null
        closeEnabled = state == Ready
        status = getStatus(connection)
        hint = getHint(connection, state)

        setButtons(state, connection)
        setError(state)

        emitState()
    }

    fun rejectProposal() {
        val proposal = proposal ?: return

        if (!connectivityManager.isConnected) {
            showNoInternetErrorLiveEvent.postValue(Unit)
            return
        }

        viewModelScope.launch {
            reject(proposal.proposerPublicKey) {
                sessionServiceState = Killed
            }
        }
    }

    fun connect() {
        val proposal = proposal ?: return

        if (!connectivityManager.isConnected) {
            showNoInternetErrorLiveEvent.postValue(Unit)
            return
        }

        if (account == null) {
            sessionServiceState = Invalid(NoSuitableAccount)
            return
        }

        viewModelScope.launch {
            try {
                approve(proposal.proposerPublicKey)
            } catch (t: Throwable) {
                WCDelegate.sessionProposalEvent = null
                showErrorLiveEvent.postValue(t.message)
            }
        }
    }

    fun disconnect() {
        if (!connectivityManager.isConnected) {
            showNoInternetErrorLiveEvent.postValue(Unit)
            return
        }

        val sessionNonNull = session ?: return

        WCDelegate.deleteSession(
            topic = sessionNonNull.topic,
            onSuccess = {
                sessionServiceState = Killed
            }
        )
    }

    suspend fun approve(proposalPublicKey: String) {
        val accountNonNull = account ?: return
        return suspendCoroutine { continuation ->
            if (Web3Wallet.getSessionProposals().isNotEmpty()) {
                val namespaces = wcManager.getSupportedNamespaces(accountNonNull)
                val sessionProposal: Wallet.Model.SessionProposal = try {
                    requireNotNull(
                        Web3Wallet.getSessionProposals()
                            .find { it.proposerPublicKey == proposalPublicKey })
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                    WCDelegate.sessionProposalEvent = null
                    return@suspendCoroutine
                }
                val sessionNamespaces = Web3Wallet.generateApprovedNamespaces(
                    sessionProposal = sessionProposal,
                    supportedNamespaces = namespaces
                )
                val approveProposal = Wallet.Params.SessionApprove(
                    proposerPublicKey = sessionProposal.proposerPublicKey,
                    namespaces = sessionNamespaces
                )

                Web3Wallet.approveSession(approveProposal,
                    onError = { error ->
                        continuation.resumeWithException(error.throwable)
                        WCDelegate.sessionProposalEvent = null
                    },
                    onSuccess = {
                        continuation.resume(Unit)
                        WCDelegate.sessionProposalEvent = null
                    })
            }
        }
    }

    suspend fun reject(proposalPublicKey: String, onSuccess: () -> Unit) {
        return suspendCoroutine { continuation ->
            if (Web3Wallet.getSessionProposals().isNotEmpty()) {
                val sessionProposal: Wallet.Model.SessionProposal = requireNotNull(
                    Web3Wallet.getSessionProposals()
                        .find { it.proposerPublicKey == proposalPublicKey })
                val rejectionReason = "Reject Session"
                val reject = Wallet.Params.SessionReject(
                    proposerPublicKey = sessionProposal.proposerPublicKey,
                    reason = rejectionReason
                )

                Web3Wallet.rejectSession(reject,
                    onSuccess = {
                        continuation.resume(Unit)
                        WCDelegate.sessionProposalEvent = null
                        onSuccess.invoke()
                    },
                    onError = { error ->
                        continuation.resumeWithException(error.throwable)
                        WCDelegate.sessionProposalEvent = null
                    })
            }
        }
    }

    private fun getStatus(connectionState: Boolean?): Status {
        return when (connectionState) {
            null -> Status.CONNECTING
            true -> Status.ONLINE
            false -> Status.OFFLINE
        }
    }

    private fun setButtons(
        state: WCSessionServiceState,
        connection: Boolean?
    ) {
        buttonStates = WCSessionButtonStates(
            connect = getConnectButtonState(state, connection),
            disconnect = getDisconnectButtonState(state, connection),
            cancel = getCancelButtonState(state),
            remove = getRemoveButtonState(state, connection),
        )
    }

    private fun getCancelButtonState(state: WCSessionServiceState): WCButtonState {
        return if (state != Ready) {
            WCButtonState.Enabled
        } else {
            WCButtonState.Hidden
        }
    }

    private fun getConnectButtonState(
        state: WCSessionServiceState,
        connectionState: Boolean?
    ): WCButtonState {
        return when {
            state == WaitingForApproveSession && connectionState == true -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getDisconnectButtonState(
        state: WCSessionServiceState,
        connectionState: Boolean?
    ): WCButtonState {
        return when {
            state == Ready && connectionState == true -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getRemoveButtonState(
        state: WCSessionServiceState,
        connectionState: Boolean?
    ): WCButtonState {
        return when {
            state is Invalid -> WCButtonState.Hidden
            connectionState == false && state is Ready -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun setError(
        state: WCSessionServiceState
    ) {
        val error: String? = when {
            state is Invalid && (state.error !is ValidationError) -> state.error.message ?: state.error::class.java.simpleName
            else -> null
        }

        showError = error
    }

    private fun getHint(connection: Boolean?, state: WCSessionServiceState): String? {
        when {
            connection == false
                    && (state == WaitingForApproveSession || state is Ready) -> {
                return Translator.getString(R.string.WalletConnect_Reconnect_Hint)
            }

            connection == null -> return null
            state is Invalid -> return getErrorMessage(state.error)
            state == WaitingForApproveSession -> Translator.getString(R.string.WalletConnect_Approve_Hint)
        }
        return null
    }

    private fun getErrorMessage(error: Throwable): String? {
        return when (error) {
            is UnsupportedChainId -> Translator.getString(R.string.WalletConnect_Error_UnsupportedChainId)
            is NoSuitableAccount -> Translator.getString(R.string.WalletConnect_Error_NoSuitableAccount)
            is NoSuitableEvmKit -> Translator.getString(R.string.WalletConnect_Error_NoSuitableEvmKit)
            is RequestNotFoundError -> Translator.getString(R.string.WalletConnect_Error_RequestNotFoundError)
            is ValidationError.UnsupportedChainNamespace -> Translator.getString(
                R.string.WalletConnect_Error_UnsupportedChains,
                error.chainNamespace)
            is ValidationError.UnsupportedChains -> Translator.getString(
                R.string.WalletConnect_Error_UnsupportedChains,
                error.chains.joinToString())

            is ValidationError.UnsupportedMethods -> Translator.getString(
                R.string.WalletConnect_Error_UnsupportedMethods,
                error.methods.joinToString())

            is ValidationError.UnsupportedEvents -> Translator.getString(
                R.string.WalletConnect_Error_UnsupportedEvents,
                error.events.joinToString())

            else -> null
        }
    }

    fun setRequestToOpen(request: Wallet.Model.SessionRequest) {
        WCDelegate.sessionRequestEvent = request
    }

}

sealed class ValidationError : Throwable() {
    class UnsupportedChainNamespace(val chainNamespace: String) : ValidationError()
    class UnsupportedChains(val chains: List<String>) : ValidationError()
    class UnsupportedMethods(val methods: List<String>) : ValidationError()
    class UnsupportedEvents(val events: List<String>) : ValidationError()
}

