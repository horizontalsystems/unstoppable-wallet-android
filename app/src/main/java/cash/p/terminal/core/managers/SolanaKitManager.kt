package cash.p.terminal.core.managers

import android.util.Log
import cash.p.terminal.core.App
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.UnsupportedException
import cash.p.terminal.core.onPollingStartedSuspend
import cash.p.terminal.core.onPollingStoppedSuspend
import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.tangem.signer.HardwareWalletSolanaAccountSigner
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.entities.HardwarePublicKey
import cash.z.ecc.android.sdk.ext.fromHex
import com.solana.core.PublicKey
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.logger.AppLogger
import io.horizontalsystems.hdwalletkit.Base58
import io.horizontalsystems.solanakit.Signer
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.network.SolanaNetworkError
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SolanaKitManager(
    private val rpcSourceManager: SolanaRpcSourceManager,
    private val walletManager: SolanaWalletManager,
    private val backgroundManager: BackgroundManager,
    private val hardwarePublicKeyStorage: HardwarePublicKeyStorage,
    private val backgroundKeepAliveManager: BackgroundKeepAliveManager,
) {

    private companion object {
        // Temporary limits to avoid too many requests problem in solan sdk
        const val limitFirstTimeTransactionCount: Int = 2
        const val limitTimeTransactionCount: Int = 2
        const val FRESH_SYNC_TIMEOUT_MS = 20_000L
    }

    private val pollingSessionCount = AtomicInteger(0)

    private val coroutineScope =
        CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.d("SolanaKitManager", "Coroutine error", throwable)
        })
    private var backgroundEventListenerJob: Job? = null
    private var rpcUpdatedJob: Job? = null
    private var tokenAccountJob: Job? = null

    var solanaKitWrapper: SolanaKitWrapper? = null

    private var useCount = 0
    var currentAccount: Account? = null
        private set
    private val solanaKitStoppedSubject = PublishSubject.create<Unit>()
    private val networkLogger = AppLogger(BlockchainType.Solana.uid).getScoped("network")

    private val mutex = Mutex()
    private val recentNetworkErrorInfoByAccountId = ConcurrentHashMap<String, Map<String, String>>()

    val kitStoppedObservable: Observable<Unit>
        get() = solanaKitStoppedSubject

    val statusInfo: Map<String, Any>?
        get() = solanaKitWrapper?.solanaKit?.statusInfo()?.let { info ->
            linkedMapOf<String, Any>().apply {
                putAll(info)
                currentNetworkErrorInfo?.let { putAll(it) }
            }
        }

    fun networkErrorInfo(accountId: String): Map<String, String>? = recentNetworkErrorInfoByAccountId[accountId]

    private val currentNetworkErrorInfo: Map<String, String>?
        get() = currentAccount?.id?.let(recentNetworkErrorInfoByAccountId::get)

    private fun handleUpdateNetwork() {
        stopKit()
        solanaKitStoppedSubject.onNext(Unit)
    }

    private suspend fun getHardwarePublicKey(accountId: String): HardwarePublicKey {
        return hardwarePublicKeyStorage.getAllPublicKeys(accountId)
            .firstOrNull { it.blockchainType == BlockchainType.Solana.uid }
            ?: throw UnsupportedException("Hardware card does not have a public key for Solana")
    }

    suspend fun getAddress(account: Account): String = when (val accountType = account.type) {
        is AccountType.Mnemonic -> Signer.address(accountType.seed)
        is AccountType.SolanaAddress -> accountType.address
        is AccountType.HardwareCard -> {
            val key = getHardwarePublicKey(account.id)
            Base58.encode(key.key.value.fromHex())
        }
        is AccountType.BitcoinAddress,
        is AccountType.EvmAddress,
        is AccountType.EvmPrivateKey,
        is AccountType.HdExtendedKey,
        is AccountType.MnemonicMonero,
        is AccountType.StellarAddress,
        is AccountType.StellarSecretKey,
        is AccountType.TonAddress,
        is AccountType.TronAddress,
        is AccountType.ZCashUfvKey -> throw UnsupportedAccountException()
    }

    suspend fun getSolanaKitWrapper(account: Account): SolanaKitWrapper = mutex.withLock {
        if (this.solanaKitWrapper != null && currentAccount != account) {
            stopKit()
            solanaKitWrapper = null
        }

        this.solanaKitWrapper?.let { existingWrapper ->
            useCount++
            return@withLock existingWrapper
        }

        val newWrapper = when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                createKitInstance(accountType, account)
            }
            is AccountType.SolanaAddress -> {
                createKitInstance(accountType, account)
            }
            is AccountType.HardwareCard -> {
                createKitInstance(account.id)
            }
            else -> throw UnsupportedAccountException()
        }

        this.solanaKitWrapper = newWrapper
        startKit()
        subscribeToEvents()
        useCount = 1
        currentAccount = account

        return@withLock newWrapper
    }

    private fun createKitInstance(
        accountType: AccountType.Mnemonic,
        account: Account
    ): SolanaKitWrapper {
        val seed = accountType.seed
        val address = Signer.address(seed)
        val signer = Signer.getInstance(seed)
        return SolanaKitWrapper(createKit(address, account.id), signer)
    }

    private fun createKitInstance(
        accountType: AccountType.SolanaAddress,
        account: Account
    ): SolanaKitWrapper {
        val address = accountType.address
        return SolanaKitWrapper(createKit(address, account.id), null)
    }

    private suspend fun createKitInstance(
        accountId: String
    ): SolanaKitWrapper {
        val hardwarePublicKey = getHardwarePublicKey(accountId)

        val signer = Signer(
            HardwareWalletSolanaAccountSigner(
                publicKey = PublicKey(hardwarePublicKey.key.value.fromHex()),
                hardwarePublicKey = hardwarePublicKey
            )
        )

        return SolanaKitWrapper(
            createKit(Base58.encode(hardwarePublicKey.key.value.fromHex()), accountId),
            signer
        )
    }

    suspend fun unlink(account: Account) = mutex.withLock {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                stopKit()
            }
        }
    }

    suspend fun startForPolling() = mutex.withLock {
        pollingSessionCount.onPollingStartedSuspend {
            solanaKitWrapper?.solanaKit?.let { kit ->
                kit.resume()
                kit.refresh()
            }
        }
    }

    suspend fun stopForPolling() = mutex.withLock {
        pollingSessionCount.onPollingStoppedSuspend(backgroundManager) {
            solanaKitWrapper?.solanaKit?.pause()
        }
    }

    /**
     * Waits for a fresh Syncing → Synced cycle. Drops the current (possibly
     * stale) Synced value, then waits for the next Synced emission that
     * arrives after a Syncing transition.
     *
     * Callers must subscribe (e.g. via `async(CoroutineStart.UNDISPATCHED)`)
     * BEFORE triggering [startForPolling], otherwise the refresh can complete
     * before the observer is active and the wait will hit timeout.
     */
    suspend fun awaitFreshSync(timeoutMs: Long = FRESH_SYNC_TIMEOUT_MS): Boolean {
        val kit = solanaKitWrapper?.solanaKit ?: return false
        return withTimeoutOrNull(timeoutMs) {
            kit.transactionsSyncStateFlow
                .dropWhile { it is SolanaKit.SyncState.Synced }
                .first { it is SolanaKit.SyncState.Synced }
            true
        } ?: false
    }

    private fun stopKit() {
        solanaKitWrapper?.solanaKit?.stop()
        solanaKitWrapper = null
        currentAccount = null
        tokenAccountJob?.cancel()
        backgroundEventListenerJob?.cancel()
        rpcUpdatedJob?.cancel()
    }

    private fun startKit() {
        solanaKitWrapper?.solanaKit?.let { kit ->
            tokenAccountJob = coroutineScope.launch {
                kit.start()
                kit.fungibleTokenAccountsFlow.collect {
                    walletManager.add(it)
                }
            }
        }
    }

    private fun createKit(address: String, walletId: String): SolanaKit =
        SolanaKit.getInstance(
            application = App.instance,
            addressString = address,
            rpcSource = rpcSourceManager.rpcSource,
            walletId = walletId,
            limitFirstTimeTransactionCount = limitFirstTimeTransactionCount,
            limitTimeTransactionCount = limitTimeTransactionCount,
            networkErrorListener = { error ->
                handleNetworkError(walletId, error)
            }
        )

    private fun handleNetworkError(accountId: String, error: SolanaNetworkError) {
        val info = linkedMapOf(
            "Recent Network Error Source" to error.source,
            "Recent Network Error Method" to error.method,
            "Recent Network Error URL" to error.url,
            "Recent Network Error Host" to error.host,
            "Recent Network Error Type" to error.throwable.javaClass.simpleName,
            "Recent Network Error Message" to error.throwable.message.orEmpty(),
        ).filterValues(String::isNotBlank).toMutableMap()

        if (error.resolvedIps.isNotEmpty()) {
            info["Recent Network Error Resolved IPs"] = error.resolvedIps.joinToString(", ")
        }

        recentNetworkErrorInfoByAccountId[accountId] = info
        val message = info.entries.joinToString(separator = "\n") { (key, value) -> "$key: $value" }
        networkLogger.warning(
            message,
            error.throwable
        )
        Timber.tag("SolanaNetwork").e(error.throwable, message)
    }

    private fun subscribeToEvents() {
        backgroundEventListenerJob = coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    solanaKitWrapper?.solanaKit?.let { kit ->
                        kit.resume()
                        delay(1000)
                        kit.refresh()
                    }
                } else if (state == BackgroundManagerState.EnterBackground) {
                    if (pollingSessionCount.get() == 0 &&
                        !backgroundKeepAliveManager.isKeepAlive(BlockchainType.Solana)
                    ) {
                        solanaKitWrapper?.solanaKit?.pause()
                    } else {
                        Timber.tag("TxPoller").d("SolanaKit staying alive")
                    }
                }
            }
        }
        rpcUpdatedJob = coroutineScope.launch {
            rpcSourceManager.rpcSourceUpdateObservable.asFlow().collect {
                handleUpdateNetwork()
            }
        }
    }
}

class SolanaKitWrapper(val solanaKit: SolanaKit, val signer: Signer?)
