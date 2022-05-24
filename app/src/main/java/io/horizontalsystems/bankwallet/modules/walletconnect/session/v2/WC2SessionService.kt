package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import com.walletconnect.walletconnectv2.client.WalletConnect
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
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

    private var proposal: WalletConnect.Model.SessionProposal? = null
    private var session: WalletConnect.Model.SettledSession? = null

    private val networkConnectionErrorSubject = PublishSubject.create<Unit>()
    val networkConnectionErrorObservable: Flowable<Unit>
        get() = networkConnectionErrorSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val stateSubject = PublishSubject.create<State>()
    val stateObservable: Flowable<State>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val allowedBlockchainsSubject = PublishSubject.create<List<WCBlockchain>>()
    val allowedBlockchainsObservable: Flowable<List<WCBlockchain>>
        get() = allowedBlockchainsSubject.toFlowable(BackpressureStrategy.BUFFER)

    var blockchains = listOf<WCBlockchain>()

    val appMetaItem: PeerMetaItem?
        get() {
            session?.peerAppMetaData?.let {
                return PeerMetaItem(
                    it.name,
                    it.url,
                    it.description,
                    it.icons.last(),
                    accountManager.activeAccount?.name,
                    false
                )
            }
            proposal?.let {
                return PeerMetaItem(
                    it.name,
                    it.url,
                    it.description,
                    it.icons.lastOrNull()?.toString(),
                    accountManager.activeAccount?.name,
                    true
                )
            }
            return null
        }

    val connectionState by pingService::state
    val connectionStateObservable by pingService::stateObservable
    val allowedBlockchains: List<WCBlockchain>
        get() = blockchains.sortedBy { it.chainId }

    private val disposables = CompositeDisposable()

    fun start() {
        topic?.let { topic ->
            val existingSession =
                sessionManager.sessions.firstOrNull { it.topic == topic } ?: return@let
            pingService.ping(existingSession.topic)
            session = existingSession
            blockchains = initialBlockchains
            allowedBlockchainsSubject.onNext(allowedBlockchains)
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
                        proposal = event.proposal
                        blockchains = initialBlockchains
                        allowedBlockchainsSubject.onNext(allowedBlockchains)
                        if (blockchains.isEmpty()) {
                            state =
                                State.Invalid(WC2SessionManager.RequestDataError.UnsupportedChainId)
                            return@subscribe
                        }
                        state = State.WaitingForApproveSession
                        pingService.receiveResponse()
                    }
                    is WC2Service.Event.SessionDeleted -> {
                        session?.topic?.let { topic ->
                            if (topic == event.deletedSession.topic) {
                                state = State.Killed
                                pingService.disconnect()
                            }
                        }
                    }
                    is WC2Service.Event.SessionSettled -> {
                        session = event.session
                        blockchains = initialBlockchains
                        allowedBlockchainsSubject.onNext(allowedBlockchains)
                        state = State.Ready
                        pingService.receiveResponse()
                    }
                    is WC2Service.Event.Error -> {
                        state = State.Invalid(event.error)
                    }
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
        if (!connectivityManager.isConnected) {
            networkConnectionErrorSubject.onNext(Unit)
            return
        }
        proposal?.let {
            service.reject(it)
            pingService.disconnect()
            state = State.Killed
        }
    }

    fun approve() {
        val proposal = proposal ?: return

        if (!connectivityManager.isConnected) {
            networkConnectionErrorSubject.onNext(Unit)
            return
        }

        if (accountManager.activeAccount == null) {
            state = State.Invalid(WC2SessionManager.RequestDataError.NoSuitableAccount)
            return
        }

        val accounts: List<String> = blockchains.filter { it.selected }.map { blockchain ->
            "eip155:${blockchain.chainId}:${blockchain.address}"
        }

        service.approve(proposal, accounts)
    }

    fun disconnect() {
        if (!connectivityManager.isConnected) {
            networkConnectionErrorSubject.onNext(Unit)
            return
        }

        val sessionNonNull = session ?: return

        state = State.Killed
        service.disconnect(sessionNonNull.topic)
        pingService.disconnect()
    }

    fun reconnect() {
        if (!connectivityManager.isConnected) {
            networkConnectionErrorSubject.onNext(Unit)
            return
        }
        session?.let {
            pingService.ping(it.topic)
        }
    }

    fun toggle(chainId: Int) {
        val blockchain = blockchains.firstOrNull { it.chainId == chainId } ?: return

        if (blockchain.selected && blockchains.filter { it.selected }.size < 2) {
            return
        }
        val toggledBlockchain = WCBlockchain(
            blockchain.chainId,
            blockchain.name,
            blockchain.address,
            !blockchain.selected,
        )
        updateItemInBlockchains(toggledBlockchain)
        allowedBlockchainsSubject.onNext(allowedBlockchains)
    }

    private fun updateItemInBlockchains(toggledBlockchain: WCBlockchain) {
        val indexToUpdate = blockchains.indexOf(toggledBlockchain)
        val updatedList = mutableListOf<WCBlockchain>()
        blockchains.forEachIndexed { index, item ->
            if (index == indexToUpdate) {
                updatedList.add(toggledBlockchain)
            } else {
                updatedList.add(item)
            }
        }
        blockchains = updatedList
    }

    private val initialBlockchains: List<WCBlockchain>
        get() {
            val account = accountManager.activeAccount ?: return emptyList()

            session?.let { session ->
                return getBlockchains(session.accounts, account)
            }

            proposal?.let { proposal ->
                return getBlockchains(proposal.chains, account)
            }
            return emptyList()
        }

    private fun getBlockchains(accounts: List<String>, account: Account): List<WCBlockchain> {
        val sessionAccountData = accounts.mapNotNull { WC2Parser.getAccountData(it) }
        return sessionAccountData.mapNotNull { data ->
            wcManager.getEvmKitWrapper(data.chain.id, account)?.let { evmKitWrapper ->
                val address = evmKitWrapper.evmKit.receiveAddress.eip55
                WCBlockchain(data.chain.id, data.chain.title, address, true)
            }
        }
    }
}
