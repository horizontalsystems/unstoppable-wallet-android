package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.BackgroundManager
import com.quantum.wallet.bankwallet.core.BackgroundManagerState
import com.quantum.wallet.bankwallet.core.UnsupportedAccountException
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.entities.AccountType
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.tronkit.TronKit
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.models.RpcSource
import io.horizontalsystems.tronkit.models.TransactionSource
import io.horizontalsystems.tronkit.network.Network
import io.horizontalsystems.tronkit.transaction.Signer
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.net.URL

class TronKitManager(
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val backgroundManager: BackgroundManager
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private val network = Network.Mainnet
    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    private val tronKitStoppedSubject = PublishSubject.create<Unit>()
    val kitStoppedObservable: Observable<Unit>
        get() = tronKitStoppedSubject

    var tronKitWrapper: TronKitWrapper? = null
        private set(value) {
            field = value
            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set

    val statusInfo: Map<String, Any>?
        get() = tronKitWrapper?.tronKit?.statusInfo()

    init {
        scope.launch {
            evmSyncSourceManager.syncSourceObservable.asFlow().collect { blockchainType ->
                if (blockchainType == BlockchainType.Tron) {
                    handleUpdateNetwork()
                }
            }
        }
    }

    private fun handleUpdateNetwork() {
        stop()
        tronKitStoppedSubject.onNext(Unit)
    }

    private fun tronRpcSource(): RpcSource {
        val syncSource = evmSyncSourceManager.getSyncSource(BlockchainType.Tron)
        val tronGridUrl = URL("https://api.trongrid.io/")
        return if (syncSource.uri.toString() == tronGridUrl.toString()) {
            RpcSource.tronGrid(network = network, apiKeys = App.appConfigProvider.trongridApiKeys)
        } else {
            RpcSource(urls = listOf(URL(syncSource.uri.toString())), auth = syncSource.auth)
        }
    }

    private fun tronTransactionSource(): TransactionSource {
        return TransactionSource.tronGrid(network = network, apiKeys = App.appConfigProvider.trongridApiKeys)
    }

    @Synchronized
    fun getTronKitWrapper(account: Account): TronKitWrapper {
        if (this.tronKitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.tronKitWrapper == null) {
            val accountType = account.type
            this.tronKitWrapper = when (accountType) {
                is AccountType.Mnemonic -> createKitInstance(accountType, account)
                is AccountType.TronAddress -> createKitInstance(accountType, account)
                is AccountType.TronPrivateKey -> createKitInstance(accountType, account)
                else -> throw UnsupportedAccountException()
            }
            start()
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.tronKitWrapper!!
    }

    private fun createKitInstance(
        accountType: AccountType.Mnemonic,
        account: Account
    ): TronKitWrapper {
        val seed = accountType.seed
        val signer = Signer.getInstance(seed, network)

        val kit = TronKit.getInstance(
            application = App.instance,
            seed = seed,
            network = network,
            rpcSource = tronRpcSource(),
            transactionSource = tronTransactionSource(),
            walletId = account.id
        )

        return TronKitWrapper(kit, signer)
    }

    private fun createKitInstance(
        accountType: AccountType.TronAddress,
        account: Account
    ): TronKitWrapper {
        val kit = TronKit.getInstance(
            application = App.instance,
            address = Address.fromBase58(accountType.address),
            network = network,
            rpcSource = tronRpcSource(),
            transactionSource = tronTransactionSource(),
            walletId = account.id
        )

        return TronKitWrapper(kit, null)
    }

    private fun createKitInstance(
        accountType: AccountType.TronPrivateKey,
        account: Account
    ): TronKitWrapper {
        val signer = Signer(accountType.key)
        val address = Signer.address(accountType.key, network)

        val kit = TronKit.getInstance(
            application = App.instance,
            address = address,
            network = network,
            rpcSource = tronRpcSource(),
            transactionSource = tronTransactionSource(),
            walletId = account.id
        )

        return TronKitWrapper(kit, signer)
    }

    @Synchronized
    fun unlink(account: Account) {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                stop()
            }
        }
    }

    private fun stop() {
        tronKitWrapper?.tronKit?.stop()
        job?.cancel()
        tronKitWrapper = null
        currentAccount = null
    }

    private fun start() {
        tronKitWrapper?.tronKit?.start()
        job = scope.launch {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> {
                        tronKitWrapper?.tronKit?.let { kit ->
                            kit.resume()
                            delay(1000)
                            kit.refresh()
                        }
                    }
                    BackgroundManagerState.EnterBackground -> {
                        tronKitWrapper?.tronKit?.pause()
                    }
                }
            }
        }
    }

    fun getAddress(type: AccountType) = when (type) {
        is AccountType.TronAddress -> type.address
        is AccountType.Mnemonic -> TronKit.getAddress(type.seed, network).base58
        else -> throw UnsupportedAccountException()
    }
}

class TronKitWrapper(val tronKit: TronKit, val signer: Signer?)
