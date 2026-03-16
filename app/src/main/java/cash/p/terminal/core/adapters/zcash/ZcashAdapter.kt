package cash.p.terminal.core.adapters.zcash

import android.content.Context
import android.database.sqlite.SQLiteDatabaseCorruptException
import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.domain.usecase.ClearZCashWalletDataUseCase
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.wallet.OneTimeReceiveAdapter
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.TokenType.AddressSpecType
import cash.z.ecc.android.sdk.CloseableSynchronizer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.fromHex
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountBalance
import cash.z.ecc.android.sdk.model.AccountCreateSetup
import cash.z.ecc.android.sdk.model.AccountImportSetup
import cash.z.ecc.android.sdk.model.AccountPurpose
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.AddressType
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.logger.AppLogger
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.math.max

class ZcashAdapter(
    context: Context,
    private val wallet: Wallet,
    private val restoreSettings: RestoreSettings,
    private val addressSpecTyped: AddressSpecType?,
    private val localStorage: ILocalStorage,
    private val backgroundManager: BackgroundManager,
    private val singleUseAddressManager: ZcashSingleUseAddressManager,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendZcashAdapter,
    OneTimeReceiveAdapter {
    private var accountBirthday = 0L
    private val existingWallet = localStorage.zcashAccountIds.contains(wallet.account.id)
    private val confirmationsThreshold = 10
    private val network: ZcashNetwork = ZcashNetwork.Mainnet
    private val lightWalletEndpoint =
        LightWalletEndpoint(host = "zec.rocks", port = 443, isSecure = true)

    private val recovering = AtomicBoolean(false)
    private val corruptionRecovery = AtomicBoolean(false)

    @Volatile
    private var synchronizer: CloseableSynchronizer
    private var transactionsProvider: ZcashTransactionsProvider
    private val clearZCashWalletDataUseCase: ClearZCashWalletDataUseCase by inject(
        ClearZCashWalletDataUseCase::class.java
    )

    private val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val accountType =
        (wallet.account.type as? AccountType.Mnemonic)
            ?: (wallet.account.type as? AccountType.ZCashUfvKey)
            ?: throw UnsupportedAccountException()

    private val seed = (accountType as? AccountType.Mnemonic)?.seed ?: ByteArray(0)

    private var zcashAccount: Account? = null

    override var receiveAddress: String

    private var statusJob: Job? = null
    private var subscriberScope: CoroutineScope? = null
    override val isMainNet: Boolean = true
    private val scope = CoroutineScope(Dispatchers.IO)

    private var balanceCheckJob: Job? = null
    private val balanceCheckMutex = kotlinx.coroutines.sync.Mutex()

    init {
        Timber.i("ZcashAdapter type $addressSpecTyped")
    }


    companion object {
        private const val DECIMAL_COUNT = 8
        val MINERS_FEE = ZcashSdk.MINERS_FEE.convertZatoshiToZec(DECIMAL_COUNT)
    }

    init {
        val walletInitMode = if (existingWallet || isWatchOnlyAccount()) {
            WalletInitMode.ExistingWallet
        } else when (wallet.account.origin) {
            AccountOrigin.Created -> WalletInitMode.NewWallet
            AccountOrigin.Restored -> WalletInitMode.RestoreWallet
        }

        val birthday = when (wallet.account.origin) {
            AccountOrigin.Created -> runBlocking {
                BlockHeight.ofLatestCheckpoint(context, network)
            }

            AccountOrigin.Restored -> restoreSettings.birthdayHeight
                ?.let { height ->
                    max(network.saplingActivationHeight.value, height)
                }
                ?.let {
                    BlockHeight.new(it)
                }
        }

        birthday?.value?.let {
            accountBirthday = it
        }

        val setup = if (!isWatchOnlyAccount()) {
            AccountCreateSetup(
                seed = FirstClassByteArray(seed),
                accountName = wallet.account.name,
                keySource = null
            )
        } else {
            null
        }

        synchronizer = Synchronizer.newBlocking(
            context = context,
            zcashNetwork = network,
            alias = clearZCashWalletDataUseCase.getValidAliasFromAccountId(
                wallet.account.id,
                addressSpecTyped
            ),
            lightWalletEndpoint = lightWalletEndpoint,
            birthday = birthday,
            walletInitMode = walletInitMode,
            setup = setup,
            isTorEnabled = localStorage.torEnabled,
            isExchangeRateEnabled = false
        )

        if (isWatchOnlyAccount()) {
            runBlocking {
                importWatchAccount()
            }
        }

        zcashAccount = runBlocking { tryOrNull { getFirstAccount() } }
        receiveAddress = runBlocking { getReceiveAddressOrEmpty() }
        transactionsProvider =
            ZcashTransactionsProvider(
                synchronizer = synchronizer as SdkSynchronizer
            )
        synchronizer.onProcessorErrorHandler = ::onProcessorError
        synchronizer.onCriticalErrorHandler = ::onCriticalError
        synchronizer.onChainErrorHandler = ::onChainError

        subscribeToEvents()
    }

    override suspend fun generateOneTimeAddress(): String? {
        val sdk = synchronizer as? SdkSynchronizer ?: return null

        return try {
            val singleUseAddress = sdk.getSingleUseTransparentAddress(getFirstAccount().accountUuid)
            singleUseAddressManager.saveNewAddress(singleUseAddress.address)
            singleUseAddress.address
        } catch (error: Exception) {
            Timber.w(error, "Failed to obtain single-use transparent address")
            singleUseAddressManager.getNextAddress()
        }
    }

    private fun isWatchOnlyAccount(): Boolean {
        return wallet.account.type is AccountType.ZCashUfvKey
    }

    private suspend fun importWatchAccount() {
        try {
            val key = (wallet.account.type as? AccountType.ZCashUfvKey)?.key
                ?: return
            (synchronizer as Synchronizer).importAccountByUfvk(
                AccountImportSetup(
                    accountName = wallet.account.name,
                    keySource = null,
                    purpose = AccountPurpose.ViewOnly,
                    ufvk = UnifiedFullViewingKey(key)
                )
            )
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to import watch-only ZCash account")
        }
    }

    private suspend fun getReceiveAddressOrEmpty(): String {
        return try {
            val account = getFirstAccount()
            when (addressSpecTyped) {
                AddressSpecType.Shielded -> synchronizer.getSaplingAddress(account)
                AddressSpecType.Transparent -> synchronizer.getTransparentAddress(account)
                AddressSpecType.Unified -> synchronizer.getUnifiedAddress(account)
                null -> synchronizer.getSaplingAddress(account)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get receive address")
            ""
        }
    }

    private fun subscribeToEvents() {
        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> {
                        start()
                    }

                    BackgroundManagerState.EnterBackground -> {
                        stop()
                    }

                    BackgroundManagerState.Unknown,
                    BackgroundManagerState.AllActivitiesDestroyed -> {

                    }
                }
            }
        }
        subscribeToStatus()
    }

    suspend fun getFirstAccount(): Account {
        return zcashAccount ?: synchronizer.getAccounts().firstOrNull() ?: throw Exception("No account found")
    }

    private var syncState: AdapterState = AdapterState.Connecting
        set(value) {
            if (value != field) {
                field = value
                adapterStateUpdatedSubject.onNext(Unit)
            }
        }

    private var lastDownloadProgress: Int = 0

    private suspend fun createNewSynchronizer() {
        val isRecovery = corruptionRecovery.get()
        val walletInitMode = if (isRecovery) {
            WalletInitMode.RestoreWallet
        } else if (existingWallet) {
            WalletInitMode.ExistingWallet
        } else when (wallet.account.origin) {
            AccountOrigin.Created -> WalletInitMode.NewWallet
            AccountOrigin.Restored -> WalletInitMode.RestoreWallet
        }

        val birthday = if (isRecovery) {
            restoreSettings.birthdayHeight
                ?.let { max(network.saplingActivationHeight.value, it) }
                ?.let { BlockHeight.new(it) }
        } else when (wallet.account.origin) {
            AccountOrigin.Created -> runBlocking {
                BlockHeight.ofLatestCheckpoint(App.instance, network)
            }

            AccountOrigin.Restored -> restoreSettings.birthdayHeight
                ?.let { height ->
                    max(network.saplingActivationHeight.value, height)
                }
                ?.let {
                    BlockHeight.new(it)
                }
        }

        birthday?.value?.let {
            accountBirthday = it
        }
        try {
            val setup = if (!isWatchOnlyAccount()) {
                AccountCreateSetup(
                    seed = FirstClassByteArray(seed),
                    accountName = wallet.account.name,
                    keySource = null
                )
            } else {
                null
            }
            synchronizer = Synchronizer.new(
                context = App.instance,
                zcashNetwork = network,
                alias = clearZCashWalletDataUseCase.getValidAliasFromAccountId(
                    wallet.account.id,
                    addressSpecTyped
                ),
                lightWalletEndpoint = lightWalletEndpoint,
                birthday = birthday,
                walletInitMode = walletInitMode,
                setup = setup,
                isTorEnabled = localStorage.torEnabled,
                isExchangeRateEnabled = false
            )

            if (isWatchOnlyAccount()) {
                importWatchAccount()
            }

        } catch (ex: Exception) {
            // To prevent crash with synchronizer creation in some situations
            // when java.lang.IllegalStateException: Another synchronizer with SynchronizerKey
            Timber.d("Synchronizer creation failed: ${ex.message}")
            stop()
            delay(3000)
            createNewSynchronizer()
            return
        }

        corruptionRecovery.set(false)
        zcashAccount = tryOrNull { getFirstAccount() }
        receiveAddress = getReceiveAddressOrEmpty()
        transactionsProvider =
            ZcashTransactionsProvider(
                synchronizer = synchronizer as SdkSynchronizer
            )
        synchronizer.onProcessorErrorHandler = ::onProcessorError
        synchronizer.onCriticalErrorHandler = ::onCriticalError
        synchronizer.onChainErrorHandler = ::onChainError
    }

    private fun subscribeToStatus() {
        statusJob?.cancel()
        statusJob = scope.launch {
            synchronizer.status.collect {
                if (it == Synchronizer.Status.SYNCED && fee.value == MINERS_FEE) {
                    calculateFee()
                }
            }
        }
    }

    override fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                startSynchronizer()
            } catch (e: IllegalStateException) {
                // Previous synchronizer still closing, wait and retry
                Timber.d("Synchronizer conflict, retrying after delay: ${e.message}")
                delay(1000)
                startSynchronizer()
            }
        }
    }

    private suspend fun startSynchronizer() {
        val sdk = synchronizer as SdkSynchronizer
        if (sdk.status.value == Synchronizer.Status.STOPPED || !sdk.coroutineScope.isActive) {
            createNewSynchronizer()
        }
        subscribe(synchronizer as SdkSynchronizer)
        subscribeToStatus()
        if (!existingWallet) {
            localStorage.zcashAccountIds += wallet.account.id
        }
    }

    override fun stop() {
        balanceCheckJob?.cancel()
        statusJob?.cancel()
        subscriberScope?.cancel()
        subscriberScope = null
        tryOrNull { synchronizer.close() }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        with(synchronizer as SdkSynchronizer) {
            refreshAllBalances()
            refreshTransactions()
        }
    }

    override val debugInfo: String
        get() = ""

    override val balanceState: AdapterState
        get() = syncState

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER).asFlow()

    override val balanceData: BalanceData
        get() = BalanceData(balance, pending = balancePending)

    override val statusInfo: Map<String, Any>
        get() {
            val statusInfo = LinkedHashMap<String, Any>()
            statusInfo["Last Block Info"] = lastBlockInfo ?: ""
            statusInfo["Sync State"] = syncState
            statusInfo["Birthday Height"] = accountBirthday
            return statusInfo
        }

    private val balance: BigDecimal
        get() {
            return with(walletBalance) {
                available.convertZatoshiToZec(DECIMAL_COUNT) +
                        pending.convertZatoshiToZec(DECIMAL_COUNT)
            }
        }

    private val walletBalance: WalletBalance
        get() {
            return when (addressSpecTyped) {
                null,
                AddressSpecType.Shielded -> synchronizer.walletBalances.value?.get(zcashAccount?.accountUuid)?.sapling
                    ?: WalletBalance(Zatoshi(0), Zatoshi(0), Zatoshi(0))

                AddressSpecType.Transparent -> WalletBalance(
                    available = synchronizer.walletBalances.value?.get(zcashAccount?.accountUuid)?.unshielded
                        ?: Zatoshi(0),
                    changePending = Zatoshi(0),
                    valuePending = Zatoshi(0)
                )

                AddressSpecType.Unified -> synchronizer.walletBalances.value?.get(zcashAccount?.accountUuid)?.orchard
                    ?: WalletBalance(Zatoshi(0), Zatoshi(0), Zatoshi(0))
            }
        }

    private val balancePending: BigDecimal
        get() {
            // TODO: Waiting when adjust option MIN_CONFIRMATIONS will appear in
            //  zcash-android-wallet-sdk
            // val walletBalance = synchronizer.saplingBalances.value ?: return BigDecimal.ZERO
            // return walletBalance.pending.convertZatoshiToZec(decimalCount)
            return BigDecimal.ZERO
        }

    override val balanceUpdatedFlow: Flow<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER).asFlow()

    override val explorerTitle: String
        get() = "blockchair.com"

    override val transactionsState: AdapterState
        get() = syncState

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val lastBlockInfo: LastBlockInfo?
        get() = synchronizer.latestHeight?.value?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = lastBlockUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)


    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> {
        val fromParams = from?.let {
            val transactionHash = it.transactionHash.fromHex().reversedArray()
            Triple(transactionHash, it.timestamp, it.transactionIndex)
        }
        return transactionsProvider.getTransactions(
            fromParams,
            transactionType,
            address,
            limit
        ).map {
            getTransactionRecord(it)
        }
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        return transactionsProvider.getNewTransactionsFlowable(transactionType, address)
            .map { transactions ->
                transactions.map { getTransactionRecord(it) }
            }
    }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/zcash/transaction/$transactionHash"

    override val maxSpendableBalance: BigDecimal
        get() {
            return with(walletBalance) {
                val available = available + pending

                val defaultFee = fee.value.convertZecToZatoshi()
                if (available <= defaultFee) {
                    BigDecimal.ZERO
                } else {
                    available.minus(defaultFee)
                        .convertZatoshiToZec(DECIMAL_COUNT)
                }
            }
        }

    private val _fee: MutableStateFlow<BigDecimal> = MutableStateFlow(MINERS_FEE)
    override val fee: StateFlow<BigDecimal> = _fee.asStateFlow()

    private suspend fun calculateFee(
        balance: Zatoshi = walletBalance.available + walletBalance.pending,
        tryCounter: Int = 4
    ): Unit = withContext(Dispatchers.IO) {
        try {
            if (balance == Zatoshi(0)) {
                _fee.value = MINERS_FEE
                return@withContext
            }
            val calculatedFee = synchronizer.proposeTransfer(
                account = zcashAccount!!,
                recipient = AppConfigProvider.donateAddresses[BlockchainType.Zcash]
                    .orEmpty(),
                amount = balance
            ).totalFeeRequired()
            _fee.value = calculatedFee.convertZatoshiToZec(DECIMAL_COUNT)
        } catch (ex: Exception) {
            if (ex is TransactionEncoderException.ProposalFromParametersException && tryCounter > 0) {
                // Not enough money to send with commission
                runCatching { // Prevent problems with negative Zatoshi
                    calculateFee(balance - MINERS_FEE.convertZecToZatoshi(), tryCounter - 1)
                }
            }
        }
    }

    override suspend fun validate(address: String): ZCashAddressType {
        if (address == receiveAddress) throw ZcashError.SendToSelfNotAllowed
        return when (synchronizer.validateAddress(address)) {
            is AddressType.Invalid -> throw ZcashError.InvalidAddress
            is AddressType.Transparent -> ZCashAddressType.Transparent
            is AddressType.Shielded -> ZCashAddressType.Shielded
            is AddressType.Tex -> ZCashAddressType.Shielded
            AddressType.Unified -> ZCashAddressType.Unified
        }
    }

    override suspend fun send(
        amount: BigDecimal,
        address: String,
        memo: String,
        logger: AppLogger?
    ): FirstClassByteArray {
        val spendingKey =
            DerivationTool.getInstance()
                .deriveUnifiedSpendingKey(seed, network, zcashAccount?.hdAccountIndex!!)
        logger?.info("call synchronizer.sendToAddress")
        val proposal = synchronizer.proposeTransfer(
            account = zcashAccount!!,
            recipient = address,
            amount = amount.convertZecToZatoshi(),
            memo = memo
        )
        return synchronizer.createProposedTransactions(
            proposal = proposal,
            usk = spendingKey
        ).first().txId
    }

    override suspend fun getOwnAddresses(): List<String> {
        val account = getFirstAccount()
        return listOfNotNull(
            tryOrNull { synchronizer.getSaplingAddress(account) },
            tryOrNull { synchronizer.getUnifiedAddress(account) }
        )
    }

    suspend fun proposeShielding(): FirstClassByteArray = withContext(Dispatchers.IO) {
        val spendingKey =
            DerivationTool.getInstance()
                .deriveUnifiedSpendingKey(seed, network, zcashAccount?.hdAccountIndex!!)
        val proposal = synchronizer.proposeShielding(
            account = zcashAccount!!,
            shieldingThreshold = Zatoshi(100000L),
            // Using empty string for memo to clear the default memo prefix value defined in
            // the SDK
            memo = "",
            // Using null will select whichever of the account's trans. receivers has funds
            // to shield
            transparentReceiver = null
        )
        if (proposal == null) {
            throw Throwable("Failed to create proposal")
        }
        synchronizer.createProposedTransactions(
            proposal = proposal,
            usk = spendingKey
        ).first().txId
    }

    private fun subscribe(synchronizer: SdkSynchronizer) {
        subscriberScope?.cancel()
        val handler = CoroutineExceptionHandler { _, exception ->
            Timber.w(exception, "Zcash synchronizer flow error")
            if (isDatabaseCorruption(exception)) {
                handleDatabaseCorruption(exception)
            }
        }
        val parentJob = synchronizer.coroutineScope.coroutineContext[Job]
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob(parentJob) + handler)
        subscriberScope = scope
        synchronizer.allTransactions.safeCollectIn(scope, transactionsProvider::onTransactions)
        synchronizer.status.safeCollectIn(scope, ::onStatus)
        synchronizer.progress.safeCollectIn(scope, ::onDownloadProgress)
        synchronizer.walletBalances.safeCollectIn(scope, ::onBalance)
        synchronizer.processorInfo.safeCollectIn(scope, ::onProcessorInfo)
    }

    private fun <T> Flow<T>.safeCollectIn(scope: CoroutineScope, block: (T) -> Unit) {
        scope.launch {
            catch { e ->
                Timber.e(e, "Zcash flow collection error")
                if (isDatabaseCorruption(e)) {
                    handleDatabaseCorruption(e)
                }
            }.collect { block(it) }
        }
    }

    private fun isDatabaseCorruption(error: Throwable): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is SQLiteDatabaseCorruptException) return true
            cause = cause.cause
        }
        return false
    }

    private fun handleDatabaseCorruption(cause: Throwable) {
        if (!recovering.compareAndSet(false, true)) return
        Timber.e(cause, "Zcash database corruption detected, recovering")
        scope.launch {
            try {
                syncState = AdapterState.NotSynced(Exception("Database corrupted, recovering"))
                try {
                    synchronizer.close()
                } catch (e: Exception) {
                    Timber.w(e, "Error closing corrupted synchronizer")
                }
                eraseWithRetry()
                corruptionRecovery.set(true)
                createNewSynchronizer()
                subscribe(synchronizer as SdkSynchronizer)
                subscribeToStatus()
            } catch (e: Exception) {
                Timber.e(e, "Zcash database corruption recovery failed")
                syncState = AdapterState.NotSynced(Exception("Recovery failed", e))
            } finally {
                recovering.set(false)
            }
        }
    }

    private suspend fun eraseWithRetry() {
        val alias = clearZCashWalletDataUseCase.getValidAliasFromAccountId(
            wallet.account.id, addressSpecTyped
        )
        repeat(3) { attempt ->
            try {
                Synchronizer.erase(App.instance, network, alias)
                return
            } catch (e: IllegalStateException) {
                if (attempt < 2) {
                    val delayMs = 1000L * (attempt + 1)
                    Timber.d("Synchronizer still active, retrying erase in ${delayMs}ms (attempt ${attempt + 1}/3)")
                    delay(delayMs)
                } else {
                    throw IllegalStateException("Failed to erase corrupted database after 3 attempts", e)
                }
            }
        }
    }

    private fun onProcessorError(error: Throwable?): Boolean {
        Timber.e(error, "Zcash processor error")
        if (error != null && isDatabaseCorruption(error)) {
            handleDatabaseCorruption(error)
            return false
        }
        return true
    }

    private fun onCriticalError(error: Throwable?): Boolean {
        Timber.e(error, "Zcash critical error")
        if (error != null && isDatabaseCorruption(error)) {
            handleDatabaseCorruption(error)
            return false
        }
        return true
    }

    private fun onChainError(errorHeight: BlockHeight, rewindHeight: BlockHeight) = Unit

    private fun onStatus(status: Synchronizer.Status) {
        syncState = when (status) {
            Synchronizer.Status.STOPPED -> AdapterState.NotSynced(Exception("stopped"))
            Synchronizer.Status.DISCONNECTED -> AdapterState.NotSynced(Exception("disconnected"))
            Synchronizer.Status.SYNCING -> AdapterState.Syncing()
            Synchronizer.Status.SYNCED -> AdapterState.Synced
            else -> syncState
        }
    }

    private fun startOneTimeAddressBalanceCheck() {
        if (balanceCheckJob?.isActive == true) return

        balanceCheckJob = scope.launch {
            try {
                balanceCheckMutex.withLock {
                    checkTransparentAddressesBalance()
                }
            } finally {
                balanceCheckJob = null
            }
        }
    }

    private fun onDownloadProgress(progress: PercentDecimal) {
        val progressPercent = progress.toPercentage()
        lastDownloadProgress = progressPercent

        syncState = AdapterState.Syncing(progress = progressPercent)
    }

    private fun onProcessorInfo(processorInfo: CompactBlockProcessor.ProcessorInfo) {
        val networkHeight = processorInfo.networkBlockHeight?.value
        val syncRange = processorInfo.overallSyncRange
        val currentSyncedHeight = syncRange?.endInclusive?.value

        if (networkHeight != null && currentSyncedHeight != null && networkHeight > currentSyncedHeight) {
            val blocksRemained = networkHeight - currentSyncedHeight
            val progress = ((currentSyncedHeight.toDouble() / networkHeight) * 100).toInt().coerceIn(0, 99)
            syncState = AdapterState.Syncing(progress = progress, blocksRemained = blocksRemained)
        } else if (lastDownloadProgress >= 100 && syncState !is AdapterState.Synced) {
            syncState = AdapterState.Syncing(progress = 100, blocksRemained = null)
        }

        lastBlockUpdatedSubject.onNext(Unit)
    }

    private fun onBalance(balance: Map<AccountUuid, AccountBalance>?) {
        balance?.get(zcashAccount?.accountUuid)?.sapling?.let {
            balanceUpdatedSubject.onNext(Unit)
        }
        startOneTimeAddressBalanceCheck()
    }

    private suspend fun checkTransparentAddressesBalance() = withContext(Dispatchers.IO) {
        val addresses = singleUseAddressManager.getAddressesForBalanceCheck()
        val sdk = synchronizer as? SdkSynchronizer ?: return@withContext

        addresses.forEach { address ->
            runCatching {
                val balance = sdk.getTransparentBalance(address)
                if (balance.value > 0) {
                    singleUseAddressManager.updateAddressBalance(address, true)
                }
            }.onFailure { error ->
                Timber.w(error, "Failed to check balance for t-address: $address")
            }
        }
    }

    private fun getTransactionRecord(transaction: ZcashTransaction): TransactionRecord {
        val transactionHashHex = transaction.transactionHash.toReversedHex()

        return if (transaction.isIncoming) {
            BitcoinTransactionRecord(
                token = wallet.token,
                uid = transactionHashHex,
                transactionHash = transactionHashHex,
                transactionIndex = transaction.transactionIndex,
                blockHeight = transaction.minedHeight?.toInt(),
                confirmationsThreshold = confirmationsThreshold,
                timestamp = transaction.timestamp,
                fee = transaction.feePaid?.convertZatoshiToZec(DECIMAL_COUNT)
                    ?.let { TransactionValue.CoinValue(wallet.token, it) },
                failed = transaction.failed,
                lockInfo = null,
                conflictingHash = null,
                showRawTransaction = false,
                amount = transaction.value.convertZatoshiToZec(DECIMAL_COUNT),
                from = null,
                to = transaction.toAddress?.let(::listOf),
                changeAddresses = null,
                memo = transaction.memo,
                source = wallet.transactionSource,
                transactionRecordType = TransactionRecordType.BITCOIN_INCOMING
            )
        } else {
            BitcoinTransactionRecord(
                token = wallet.token,
                uid = transactionHashHex,
                transactionHash = transactionHashHex,
                transactionIndex = transaction.transactionIndex,
                blockHeight = transaction.minedHeight?.toInt(),
                confirmationsThreshold = confirmationsThreshold,
                timestamp = transaction.timestamp,
                fee = transaction.feePaid?.let { it.convertZatoshiToZec(DECIMAL_COUNT) }
                    ?.let { TransactionValue.CoinValue(wallet.token, it) },
                failed = transaction.failed,
                lockInfo = null,
                conflictingHash = null,
                showRawTransaction = false,
                amount = transaction.value.convertZatoshiToZec(DECIMAL_COUNT).negate(),
                to = transaction.toAddress?.let(::listOf),
                from = null,
                changeAddresses = null,
                sentToSelf = false,
                memo = transaction.memo,
                source = wallet.transactionSource,
                replaceable = false,
                transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING
            )
        }
    }

    enum class ZCashAddressType {
        Shielded, Transparent, Unified
    }

    sealed class ZcashError : Exception() {
        object InvalidAddress : ZcashError()
        object SendToSelfNotAllowed : ZcashError()
    }
}

object ZcashAddressValidator {
    fun validate(address: String): Boolean {
        return isValidZcashAddress(address)
    }

    fun isTransparentAddress(address: String): Boolean {
        return isValidTransparentAddress(address)
    }

    private fun isValidTransparentAddress(address: String): Boolean {
        val transparentPattern = Pattern.compile("^t[0-9a-zA-Z]{34}$")
        return transparentPattern.matcher(address).matches()
    }

    private fun isValidShieldedAddress(address: String): Boolean {
        val shieldedPattern = Pattern.compile("^z[0-9a-zA-Z]{77}$")
        return shieldedPattern.matcher(address).matches()
    }

    private fun isValidUnifiedAddress(address: String): Boolean {
        val unifiedPattern = Pattern.compile("^u1[qpzry9x8gf2tvdw0s3jn54khce6mua7l]{100,220}$")
        return unifiedPattern.matcher(address).matches()
    }

    private fun isValidZcashAddress(address: String): Boolean {
        return isValidTransparentAddress(address) || isValidShieldedAddress(address) || isValidUnifiedAddress(address)
    }
}
