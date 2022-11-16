package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import com.walletconnect.sign.client.Sign
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule.PeerMetaItem
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WC2SessionService(
    private val service: WC2Service,
    private val wcManager: WC2Manager,
    private val sessionManager: WC2SessionManager,
    private val accountManager: IAccountManager,
    private val pingService: WC2PingService,
    private val connectivityManager: ConnectivityManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val topic: String?,
    private val connectionLink: String?
) {

    sealed class State {
        object Idle : State()
        class Invalid(val error: Throwable) : State()
        object WaitingForApproveSession : State()
        object Ready : State()
        object Killed : State()
    }

    var state: State = State.Idle
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    private var proposal: Sign.Model.SessionProposal? = null
    private var session: Sign.Model.Session? = null

    private val stateSubject = PublishSubject.create<State>()
    val stateObservable: Flowable<State>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    var blockchains = listOf<WCBlockchain>()
    var appMetaItem: PeerMetaItem? = null
    val connectionState by pingService::state
    val connectionStateObservable by pingService::stateObservable

    private val disposables = CompositeDisposable()

    fun start() {
        topic?.let { topic ->
            val existingSession =
                sessionManager.sessions.firstOrNull { it.topic == topic } ?: return@let
            pingService.ping(existingSession.topic)
            appMetaItem = existingSession.metaData?.let {
                PeerMetaItem(
                    it.name,
                    it.url,
                    it.description,
                    it.icons.lastOrNull()?.toString(),
                    accountManager.activeAccount?.name,
                )
            }

            session = existingSession
            blockchains = getBlockchainsBySession(existingSession)
            state = State.Ready
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

        service.eventObservable
            .subscribe { event ->
                when (event) {
                    is WC2Service.Event.WaitingForApproveSession -> {
                        val sessionProposal = event.proposal
                        appMetaItem = PeerMetaItem(
                            sessionProposal.name,
                            sessionProposal.url,
                            sessionProposal.description,
                            sessionProposal.icons.lastOrNull()?.toString(),
                            accountManager.activeAccount?.name,
                        )

                        proposal = sessionProposal

                        try {
                            blockchains = getBlockchainsByProposal(sessionProposal)

                            state = State.WaitingForApproveSession
                            pingService.receiveResponse()
                        } catch (e: WC2SessionManager.RequestDataError) {
                            state = State.Invalid(e)
                        }
                    }
                    is WC2Service.Event.SessionDeleted -> {
                        val deletedSession = event.deletedSession
                        if (deletedSession is Sign.Model.DeletedSession.Success) {
                            session?.topic?.let { topic ->
                                if (topic == deletedSession.topic) {
                                    state = State.Killed
                                    pingService.disconnect()
                                }
                            }
                        }
                    }
                    is WC2Service.Event.SessionSettled -> {
                        val session = event.session
                        appMetaItem = session.metaData?.let {
                            PeerMetaItem(
                                it.name,
                                it.url,
                                it.description,
                                it.icons.lastOrNull()?.toString(),
                                accountManager.activeAccount?.name,
                            )
                        }

                        this.session = session
                        blockchains = getBlockchainsBySession(session)
                        state = State.Ready
                        pingService.receiveResponse()
                    }
                    is WC2Service.Event.Error -> {
                        state = State.Invalid(event.error)
                    }
                    WC2Service.Event.Default,
                    WC2Service.Event.Ready -> {}
                }
            }
            .let {
                disposables.add(it)
            }

        connectionLink?.let {
            service.pair(it)
        }
    }

    fun stop() {
        disposables.clear()
    }

    fun reject() {
        val proposal = proposal ?: return

        if (!connectivityManager.isConnected) {
            throw NoInternetException()
        }

        service.reject(proposal)
        pingService.disconnect()
        state = State.Killed
    }

    fun approve() {
        val proposal = proposal ?: return

        if (!connectivityManager.isConnected) {
            throw NoInternetException()
        }

        if (accountManager.activeAccount == null) {
            state = State.Invalid(WC2SessionManager.RequestDataError.NoSuitableAccount)
            return
        }

        service.approve(proposal, blockchains)
    }

    fun disconnect() {
        if (!connectivityManager.isConnected) {
            throw NoInternetException()
        }

        val sessionNonNull = session ?: return

        state = State.Killed
        service.disconnect(sessionNonNull.topic)
        pingService.disconnect()
    }

    fun reconnect() {
        if (!connectivityManager.isConnected) {
            throw NoInternetException()
        }
        session?.let {
            pingService.ping(it.topic)
        }
    }

    private fun getBlockchainsByProposal(proposal: Sign.Model.SessionProposal): List<WCBlockchain> {
        val account = accountManager.activeAccount ?: throw WC2SessionManager.RequestDataError.NoSuitableAccount
        val chains = proposal.requiredNamespaces.values.map { it.chains }.flatten()
        val blockchains = getBlockchains(chains, account)
        if (blockchains.size < chains.size) {
            throw WC2SessionManager.RequestDataError.UnsupportedChainId
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
                val chainName = evmBlockchainManager.getBlockchain(data.chain.id)?.name ?: data.chain.name
                WCBlockchain(data.chain.id, chainName, address)
            }
        }
    }
}

class NoInternetException: Throwable("No Internet")
