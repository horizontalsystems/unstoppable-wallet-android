package cash.p.terminal.core.managers

import android.util.Log
import androidx.room.concurrent.AtomicInt
import cash.p.terminal.core.App
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.adapters.MoneroAdapter
import cash.p.terminal.core.utils.MoneroConfig
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import com.m2049r.xmrwallet.data.TxData
import com.m2049r.xmrwallet.data.UserNotes
import com.m2049r.xmrwallet.model.PendingTransaction
import com.m2049r.xmrwallet.model.TransactionInfo
import com.m2049r.xmrwallet.model.Wallet
import com.m2049r.xmrwallet.model.Wallet.ConnectionStatus
import com.m2049r.xmrwallet.model.WalletManager
import com.m2049r.xmrwallet.service.MoneroWalletService
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal

class MoneroKitManager(
    private val moneroWalletService: MoneroWalletService,
    private val backgroundManager: BackgroundManager
) {
    private val connectivityManager = App.connectivityManager
    private val coroutineScope =
        CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.d("MoneroKitManager", "Coroutine error", throwable)
        })
    var moneroKitWrapper: MoneroKitWrapper? = null

    private var useCount = AtomicInt(0)
    var currentAccount: Account? = null
        private set
    private val moneroKitStoppedSubject = PublishSubject.create<Unit>()

    val kitStoppedObservable: Observable<Unit>
        get() = moneroKitStoppedSubject

    suspend fun getMoneroKitWrapper(account: Account): MoneroKitWrapper {
        if (this.moneroKitWrapper != null && currentAccount != account) {
            stopKit()
            moneroKitWrapper = null
        }

        if (this.moneroKitWrapper == null) {
            val accountType = account.type
            this.moneroKitWrapper = when (accountType) {
                is AccountType.MnemonicMonero -> {
                    createKitInstance(accountType)
                }

                else -> throw UnsupportedAccountException()
            }
            startKit()
            subscribeToEvents()
            useCount.set(0)
            currentAccount = account
        }

        useCount.incrementAndGet()
        return this.moneroKitWrapper!!
    }

    private fun createKitInstance(
        accountType: AccountType.MnemonicMonero,
    ): MoneroKitWrapper {
        return MoneroKitWrapper(
            moneroWalletService = moneroWalletService,
            accountType = accountType
        )
    }

    suspend fun unlinkAll() {
        currentAccount = null
        useCount.set(0)
        stopKit()
    }

    suspend fun unlink(account: Account) {
        if (account == currentAccount) {
            if (useCount.decrementAndGet() < 1) {
                stopKit()
            }
        }
    }

    private suspend fun stopKit() {
        currentAccount = null
        moneroKitWrapper?.stop()
    }

    private suspend fun startKit() {
        moneroKitWrapper?.start()
    }

    private fun subscribeToEvents() {
        coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    startKit()
                } else if (state == BackgroundManagerState.EnterBackground) {
                    stopKit()
                }
            }
        }
        coroutineScope.launch {
            connectivityManager.networkAvailabilityFlow.collect { connected ->
                println("Connection status: $connected")
                if (connected) {
                    startKit()
                }
            }
        }
    }
}

class MoneroKitWrapper(
    private val moneroWalletService: MoneroWalletService,
    private val accountType: AccountType.MnemonicMonero
) : MoneroWalletService.Observer {

    companion object {
        private const val TAG = "MoneroKitWrapper"
    }

    private var isStarted = false

    private val _syncState = MutableStateFlow<AdapterState>(AdapterState.Syncing())
    val syncState = _syncState.asStateFlow()

    private val _lastBlockInfoFlow = MutableStateFlow<LastBlockInfo?>(null)
    val lastBlockInfoFlow = _lastBlockInfoFlow.asStateFlow()

    private val _transactionsStateUpdatedFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val transactionsStateUpdatedFlow = _transactionsStateUpdatedFlow.asSharedFlow()

    suspend fun start() = withContext(Dispatchers.IO) {
        if (!isStarted) {
            try {
                MoneroConfig.autoSelectNode()?.let {
                    WalletManager.getInstance()
                        .setDaemon(it)
                } ?: throw IllegalStateException("No nodes available")

                moneroWalletService.setObserver(this@MoneroKitWrapper)
                moneroWalletService.start(accountType.walletInnerName, accountType.password)
                isStarted = true
            } catch (e: Exception) {
                _syncState.value = AdapterState.NotSynced(e)
                Log.e("MoneroKitWrapper", "Failed to start Monero wallet", e)
            }
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        if (isStarted) {
            try {
                isStarted = false
                moneroWalletService.stop()
            } catch (e: Exception) {
                Log.e("MoneroKitWrapper", "Failed to stop Monero wallet", e)
            }
        }
    }

    suspend fun refresh() {
        if (isStarted) {
            try {
                stop()
                start()
            } catch (e: Exception) {
                Log.e("MoneroKitWrapper", "Failed to refresh Monero wallet", e)
            }
        }
    }

    fun send(
        amount: BigDecimal,
        address: String,
        memo: String?
    ) {
        val txData = buildTxData(amount, address, memo)

        moneroWalletService.prepareTransaction("send", txData)
        moneroWalletService.sendTransaction(memo)
    }

    fun estimateFee(
        amount: BigDecimal,
        address: String,
        memo: String?
    ): Long {
        val txData = buildTxData(amount, address, memo)
        return moneroWalletService.wallet!!.estimateTransactionFee(txData)
    }

    private fun buildTxData(
        amount: BigDecimal,
        address: String,
        memo: String?
    ) = TxData().apply {
        this.destination = address
        this.amount = amount.movePointRight(MoneroAdapter.decimal).toLong()
        this.mixin = moneroWalletService.wallet!!.defaultMixin
        this.priority = PendingTransaction.Priority.Priority_Default
        memo?.let {
            this.userNotes = UserNotes(it)
        }
    }

    fun statusInfo(): Map<String, Any> {
        return mapOf(
            "walletName" to accountType.walletInnerName,
            "isStarted" to isStarted
        )
    }

    // Add methods for balance, transactions, etc.
    fun getBalance(): Long {
        return try {
            Timber.tag(TAG).d("getBalance: ${moneroWalletService.wallet?.balance}")
            moneroWalletService.wallet?.balance ?: 0L
        } catch (e: Exception) {
            Timber.tag(TAG).d("getBalance: Failed to get balance")
            0L
        }
    }

    fun getAddress(): String {
        return try {
            moneroWalletService.wallet!!.address
        } catch (e: Exception) {
            Log.e("MoneroKitWrapper", "Failed to get address", e)
            ""
        }
    }

    fun getTransactions(): List<TransactionInfo> {
        return try {
            moneroWalletService.wallet?.history?.all ?: emptyList()
        } catch (e: Exception) {
            Timber.tag(TAG).d("getTransactions: Failed to get transactions")
            emptyList()
        }
    }

    override fun onRefreshed(
        wallet: Wallet?,
        full: Boolean
    ): Boolean {
        if (moneroWalletService.connectionStatus == ConnectionStatus.ConnectionStatus_Connected) {
            _lastBlockInfoFlow.value = if (moneroWalletService.daemonHeight != 0L) {
                LastBlockInfo(moneroWalletService.daemonHeight.toInt())
            } else {
                null
            }
        }

        if (moneroWalletService.connectionStatus == ConnectionStatus.ConnectionStatus_Connected) {
            _transactionsStateUpdatedFlow.tryEmit(Unit)
        }

        _syncState.value =
            if (moneroWalletService.connectionStatus != ConnectionStatus.ConnectionStatus_Connected) {
                println("MoneroKitWrapper: Not connected")
                AdapterState.NotSynced(IllegalStateException("Not connected"))
            } else if (moneroWalletService.wallet?.isSynchronized == true) {
                println("MoneroKitWrapper: Synced")
                AdapterState.Synced
            } else {
                println("MoneroKitWrapper: Sync in progress")
                val progressPercent = runCatching {
                    val currentHeight = moneroWalletService.wallet?.blockChainHeight ?: 0
                    val totalHeight = WalletManager.getInstance().blockchainHeight

                    if (totalHeight > 0) {
                        ((currentHeight * 100) / totalHeight).coerceIn(0, 100)
                    } else {
                        0
                    }
                }.getOrElse { 0 }

                AdapterState.Syncing(progressPercent.toInt())
            }
        Timber.tag(TAG)
            .d("onRefreshed, isSynchronized = ${wallet?.isSynchronized}, connectionStatus = ${wallet?.connectionStatus}, full = $full")
        return true
    }

    override fun onProgress(text: String?) {
        Timber.tag(TAG).d("onProgress: $text")
    }

    override fun onProgress(n: Int) {
        Timber.tag(TAG).d("onProgress: $n")
    }

    override fun onWalletStored(success: Boolean) {
        Timber.tag(TAG).d("onWalletStored: $success")
    }

    override fun onTransactionCreated(
        tag: String?,
        pendingTransaction: PendingTransaction?
    ) {
        Timber.tag(TAG).d("onTransactionCreated: $tag, $pendingTransaction")
    }

    override fun onTransactionSent(txid: String?) {
        Timber.tag(TAG).d("onTransactionSent: $txid")
    }

    override fun onSendTransactionFailed(error: String?) {
        Timber.tag(TAG).d("onSendTransactionFailed: $error")
    }

    override fun onWalletStarted(walletStatus: Wallet.Status?) {
        Timber.tag(TAG).d("onWalletStarted: $walletStatus")
        if (moneroWalletService.wallet?.isSynchronized == true) {
            println("MoneroKitWrapper: Synced")
            _syncState.value = AdapterState.Synced
        }
    }

    override fun onWalletOpen(device: Wallet.Device?) {
        Timber.tag(TAG).d("onWalletOpen: $device")
    }
}
