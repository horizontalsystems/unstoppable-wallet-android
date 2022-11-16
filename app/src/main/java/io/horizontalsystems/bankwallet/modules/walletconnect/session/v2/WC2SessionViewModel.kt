package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.walletconnect.sign.client.Sign
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionServiceState.*
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.*
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager.RequestDataError.*
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class WC2SessionViewModel(
    private val wc2service: WC2Service,
    private val wcManager: WC2Manager,
    private val sessionManager: WC2SessionManager,
    private val accountManager: IAccountManager,
    private val pingService: WC2PingService,
    private val connectivityManager: ConnectivityManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val topic: String?,
    private val connectionLink: String?
) : ViewModel() {

    private val TAG = "WC2SessionViewModel"

    val closeLiveEvent = SingleLiveEvent<Unit>()
    val showErrorLiveEvent = SingleLiveEvent<Unit>()

    private val disposables = CompositeDisposable()

    var peerMeta by mutableStateOf<WCSessionModule.PeerMetaItem?>(null)
        private set

    var invalidUrlError by mutableStateOf(false)
        private set

    var closeEnabled by mutableStateOf(false)
        private set

    var connecting by mutableStateOf(false)
        private set

    var buttonStates by mutableStateOf<WCSessionButtonStates?>(null)
        private set

    var hint by mutableStateOf<Int?>(null)
        private set

    var showError by mutableStateOf<String?>(null)
        private set

    var blockchains by mutableStateOf<List<WC2SessionModule.BlockchainViewItem>>(listOf())
        private set

    var status by mutableStateOf<WCSessionViewModel.Status?>(null)
        private set

    private var sessionServiceState: WC2SessionServiceState = WC2SessionServiceState.Idle
        set(value) {
            field = value

            sync(state = value, connection = pingService.state)
        }

    private var proposal: Sign.Model.SessionProposal? = null
    private var session: Sign.Model.Session? = null

    private var wcBlockchains = listOf<WCBlockchain>()
    private var appMetaItem: WCSessionModule.PeerMetaItem? = null

    init {
        pingService.stateObservable
            .subscribeIO {
                Log.e(TAG, "sync from connection change: $it")
                sync(state = sessionServiceState, connection = it)
            }
            .let {
                disposables.add(it)
            }

        topic?.let { topic ->
            val existingSession =
                sessionManager.sessions.firstOrNull { it.topic == topic } ?: return@let
            pingService.ping(existingSession.topic)
            appMetaItem = existingSession.metaData?.let {
                WCSessionModule.PeerMetaItem(
                    it.name,
                    it.url,
                    it.description,
                    it.icons.lastOrNull()?.toString(),
                    accountManager.activeAccount?.name,
                )
            }

            session = existingSession
            wcBlockchains = getBlockchainsBySession(existingSession)
            sessionServiceState = WC2SessionServiceState.Ready
        }

        connectivityManager.networkAvailabilitySignal
            .subscribeOn(Schedulers.io())
            .subscribe {
                if (!connectivityManager.isConnected) {
                    pingService.disconnect()
                } else {
                    session?.let {
                        pingService.ping(it.topic)
                    }
                }
            }
            .let {
                disposables.add(it)
            }

        wc2service.eventObservable
            .subscribe { event ->
                when (event) {
                    is WC2Service.Event.WaitingForApproveSession -> {
                        val sessionProposal = event.proposal
                        appMetaItem = WCSessionModule.PeerMetaItem(
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
                            pingService.receiveResponse()
                        } catch (e: WC2SessionManager.RequestDataError) {
                            sessionServiceState = Invalid(e)
                        }
                    }
                    is WC2Service.Event.SessionDeleted -> {
                        val deletedSession = event.deletedSession
                        if (deletedSession is Sign.Model.DeletedSession.Success) {
                            session?.topic?.let { topic ->
                                if (topic == deletedSession.topic) {
                                    sessionServiceState = Killed
                                    pingService.disconnect()
                                }
                            }
                        }
                    }
                    is WC2Service.Event.SessionSettled -> {
                        val session = event.session
                        appMetaItem = session.metaData?.let {
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
                        pingService.receiveResponse()
                    }
                    is WC2Service.Event.Error -> {
                        sessionServiceState = Invalid(event.error)
                    }
                    WC2Service.Event.Default,
                    WC2Service.Event.Ready -> {
                    }
                }
            }
            .let {
                disposables.add(it)
            }

        connectionLink?.let {
            wc2service.pair(it)
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    private fun sync(
        state: WC2SessionServiceState,
        connection: WC2PingServiceState
    ) {
        val allowedBlockchains = wcBlockchains.sortedBy { it.chainId }

        if (state == Killed) {
            closeLiveEvent.postValue(Unit)
            return
        }

        peerMeta = appMetaItem
        blockchains = getBlockchainViewItems(allowedBlockchains)
        connecting = connection == WC2PingServiceState.Connecting
        closeEnabled = state == Ready
        status = getStatus(connection)
        hint = getHint(connection, state)

        setButtons(state, connection)
        setError(state)
    }

    fun cancel() {
        val proposal = proposal ?: return

        if (!connectivityManager.isConnected) {
            showErrorLiveEvent.postValue(Unit)
            return
        }

        wc2service.reject(proposal)
        pingService.disconnect()
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
        pingService.disconnect()
    }

    fun reconnect() {
        if (!connectivityManager.isConnected) {
            showErrorLiveEvent.postValue(Unit)
            return
        }
        session?.let {
            pingService.ping(it.topic)
        }
    }

    private fun getBlockchainViewItems(blockchains: List<WCBlockchain>) = blockchains.map {
        WC2SessionModule.BlockchainViewItem(
            it.chainId,
            it.name,
            it.address.shorten(),
        )
    }

    private fun getStatus(connectionState: WC2PingServiceState): WCSessionViewModel.Status? {
        return when (connectionState) {
            WC2PingServiceState.Connecting -> WCSessionViewModel.Status.CONNECTING
            WC2PingServiceState.Connected -> WCSessionViewModel.Status.ONLINE
            is WC2PingServiceState.Disconnected -> WCSessionViewModel.Status.OFFLINE
        }
    }

    private fun setButtons(
        state: WC2SessionServiceState,
        connection: WC2PingServiceState
    ) {
        Log.e(TAG, "setButtons: ${state}, ${connection}")
        buttonStates = WCSessionButtonStates(
            connect = getConnectButtonState(state, connection),
            disconnect = getDisconnectButtonState(state, connection),
            cancel = getCancelButtonState(state),
            reconnect = getReconnectButtonState(state, connection),
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
        connectionState: WC2PingServiceState
    ): WCButtonState {
        return when {
            state == WaitingForApproveSession && connectionState == WC2PingServiceState.Connected -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getDisconnectButtonState(
        state: WC2SessionServiceState,
        connectionState: WC2PingServiceState
    ): WCButtonState {
        return when {
            state == Ready && connectionState == WC2PingServiceState.Connected -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getReconnectButtonState(
        state: WC2SessionServiceState,
        connectionState: WC2PingServiceState
    ): WCButtonState {
        return when {
            state is Invalid -> WCButtonState.Hidden
            connectionState is WC2PingServiceState.Disconnected -> WCButtonState.Enabled
            connectionState is WC2PingServiceState.Connecting -> WCButtonState.Disabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getRemoveButtonState(
        state: WC2SessionServiceState,
        connectionState: WC2PingServiceState
    ): WCButtonState {
        return when {
            state is Invalid -> WCButtonState.Hidden
            connectionState is WC2PingServiceState.Disconnected && state is Ready -> WCButtonState.Enabled
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

    private fun getHint(connection: WC2PingServiceState, state: WC2SessionServiceState): Int? {
        when {
            connection is WC2PingServiceState.Disconnected
                && (state == WaitingForApproveSession || state is Ready) -> {
                return R.string.WalletConnect_Reconnect_Hint
            }
            connection == WC2PingServiceState.Connecting -> return null
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
            is DataParsingError -> R.string.WalletConnect_Error_DataParsingError
            is RequestNotFoundError -> R.string.WalletConnect_Error_RequestNotFoundError
            else -> null
        }
    }


//  session service

    private fun getBlockchainsByProposal(proposal: Sign.Model.SessionProposal): List<WCBlockchain> {
        val account = accountManager.activeAccount
            ?: throw NoSuitableAccount
        val chains = proposal.requiredNamespaces.values.map { it.chains }.flatten()
        val blockchains = getBlockchains(chains, account)
        if (blockchains.size < chains.size) {
            throw UnsupportedChainId
        }
        return blockchains
    }

    private fun getBlockchainsBySession(session: Sign.Model.Session): List<WCBlockchain> {
        val account = accountManager.activeAccount ?: return emptyList()
        val accounts = session.namespaces.map { it.value.accounts }.flatten()
        return getBlockchains(accounts, account)
    }

    private fun getBlockchains(accounts: List<String>, account: Account): List<WCBlockchain> {
        val sessionAccountData = accounts.mapNotNull { WC2Parser.getAccountData(it) }
        return sessionAccountData.mapNotNull { data ->
            wcManager.getEvmKitWrapper(data.chain.id, account)?.let { evmKitWrapper ->
                val address = evmKitWrapper.evmKit.receiveAddress.eip55
                val chainName =
                    evmBlockchainManager.getBlockchain(data.chain.id)?.name ?: data.chain.name
                WCBlockchain(data.chain.id, chainName, address)
            }
        }
    }


}
