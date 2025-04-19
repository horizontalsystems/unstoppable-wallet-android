package cash.p.terminal.core.adapters.zcash

import android.content.Context
import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.managers.RestoreSettings
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
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.fromHex
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountBalance
import cash.z.ecc.android.sdk.model.AccountCreateSetup
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.PercentDecimal
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.regex.Pattern
import kotlin.math.max

class ZcashAdapter(
    context: Context,
    private val wallet: Wallet,
    private val restoreSettings: RestoreSettings,
    private val addressSpecTyped: AddressSpecType?,
    private val localStorage: ILocalStorage,
    private val backgroundManager: BackgroundManager,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendZcashAdapter {
    private var accountBirthday = 0L
    private val existingWallet = localStorage.zcashAccountIds.contains(wallet.account.id)
    private val confirmationsThreshold = 10
    private val network: ZcashNetwork = ZcashNetwork.Mainnet
    private val lightWalletEndpoint =
        LightWalletEndpoint(host = "zec.rocks", port = 443, isSecure = true)

    private var synchronizer: CloseableSynchronizer
    private var transactionsProvider: ZcashTransactionsProvider

    private val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val accountType =
        (wallet.account.type as? AccountType.Mnemonic) ?: throw UnsupportedAccountException()
    private val seed = accountType.seed

    private var zcashAccount: Account? = null

    override var receiveAddress: String

    private var statusJob: Job? = null
    override val isMainNet: Boolean = true
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        println("ZcashAdapter type $addressSpecTyped")
    }


    companion object {
        private const val ALIAS_PREFIX = "zcash_"

        private fun getValidAliasFromAccountId(
            accountId: String,
            addressSpecTyped: AddressSpecType?
        ): String {
            return (ALIAS_PREFIX + accountId.replace("-", "_")).let {
                if (addressSpecTyped != null) {
                    it + "_${addressSpecTyped.name}"
                } else {
                    it
                }
            }
        }

        private const val DECIMAL_COUNT = 8
        val MINERS_FEE = ZcashSdk.MINERS_FEE.convertZatoshiToZec(DECIMAL_COUNT)

        fun clear(accountId: String) {
            runBlocking {
                Synchronizer.erase(
                    appContext = App.instance,
                    network = ZcashNetwork.Mainnet,
                    alias = getValidAliasFromAccountId(accountId, null)
                )
                AddressSpecType.entries.forEach {
                    Synchronizer.erase(
                        appContext = App.instance,
                        network = ZcashNetwork.Mainnet,
                        alias = getValidAliasFromAccountId(accountId, it)
                    )
                }
            }
        }
    }

    init {
        val walletInitMode = if (existingWallet) {
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

        synchronizer = Synchronizer.newBlocking(
            context = context,
            zcashNetwork = network,
            alias = getValidAliasFromAccountId(wallet.account.id, addressSpecTyped),
            lightWalletEndpoint = lightWalletEndpoint,
            birthday = birthday,
            walletInitMode = walletInitMode,
            setup = AccountCreateSetup(
                seed = FirstClassByteArray(seed),
                accountName = wallet.account.name,
                keySource = null
            )
        )

        zcashAccount = runBlocking { getFirstAccount() }
        receiveAddress = runBlocking {
            when (addressSpecTyped) {
                AddressSpecType.Shielded -> synchronizer.getSaplingAddress(getFirstAccount())
                AddressSpecType.Transparent -> synchronizer.getTransparentAddress(getFirstAccount())
                AddressSpecType.Unified -> synchronizer.getUnifiedAddress(getFirstAccount())
                null -> synchronizer.getSaplingAddress(getFirstAccount())
            }
        }
        transactionsProvider =
            ZcashTransactionsProvider(
                receiveAddress = receiveAddress,
                synchronizer = synchronizer as SdkSynchronizer
            )
        synchronizer.onProcessorErrorHandler = ::onProcessorError
        synchronizer.onChainErrorHandler = ::onChainError

        subscribeToEvents()
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


    private suspend fun getFirstAccount(): Account {
        return synchronizer.getAccounts().firstOrNull() ?: throw Exception("No account found")
    }

    private var syncState: AdapterState = AdapterState.Syncing()
        set(value) {
            if (value != field) {
                field = value
                adapterStateUpdatedSubject.onNext(Unit)
            }
        }

    private fun createNewSynchronizer() {
        val walletInitMode = if (existingWallet) {
            WalletInitMode.ExistingWallet
        } else when (wallet.account.origin) {
            AccountOrigin.Created -> WalletInitMode.NewWallet
            AccountOrigin.Restored -> WalletInitMode.RestoreWallet
        }

        val birthday = when (wallet.account.origin) {
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

        synchronizer = Synchronizer.newBlocking(
            context = App.instance,
            zcashNetwork = network,
            alias = getValidAliasFromAccountId(wallet.account.id, addressSpecTyped),
            lightWalletEndpoint = lightWalletEndpoint,
            birthday = birthday,
            walletInitMode = walletInitMode,
            setup = AccountCreateSetup(
                seed = FirstClassByteArray(seed),
                accountName = wallet.account.name,
                keySource = null
            )
        )

        zcashAccount = runBlocking { getFirstAccount() }
        receiveAddress = runBlocking {
            when (addressSpecTyped) {
                AddressSpecType.Shielded -> synchronizer.getSaplingAddress(getFirstAccount())
                AddressSpecType.Transparent -> synchronizer.getTransparentAddress(getFirstAccount())
                AddressSpecType.Unified -> synchronizer.getUnifiedAddress(getFirstAccount())
                null -> synchronizer.getSaplingAddress(getFirstAccount())
            }
        }
        transactionsProvider =
            ZcashTransactionsProvider(
                receiveAddress = receiveAddress,
                synchronizer = synchronizer as SdkSynchronizer
            )
        synchronizer.onProcessorErrorHandler = ::onProcessorError
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
            delay(1000) //We need this delay to wait until previous synchronizer is closed
            if ((synchronizer as SdkSynchronizer).status.value == Synchronizer.Status.STOPPED) {
                createNewSynchronizer()
            }
            subscribe(synchronizer as SdkSynchronizer)
            if (!existingWallet) {
                localStorage.zcashAccountIds += wallet.account.id
            }
        }
    }

    override fun stop() {
        synchronizer.close()
    }

    override fun refresh() = Unit

    override val debugInfo: String
        get() = ""

    override val balanceState: AdapterState
        get() = syncState

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER).asFlow()

    override val balanceData: BalanceData
        get() = BalanceData(balance, pending = balancePending)

    val statusInfo: Map<String, Any>
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

    override fun sendAllowed(): Boolean {
        return balanceState is AdapterState.Synced || balanceState is AdapterState.Syncing
    }

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
            }.asFlow()
    }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/zcash/transaction/$transactionHash"

    override val availableBalance: BigDecimal
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
                recipient = App.appConfigProvider.donateAddresses[BlockchainType.Zcash]
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
        logger: AppLogger
    ): FirstClassByteArray {
        val spendingKey =
            DerivationTool.getInstance()
                .deriveUnifiedSpendingKey(seed, network, zcashAccount?.hdAccountIndex!!)
        logger.info("call synchronizer.sendToAddress")
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

    // Subscribe to a synchronizer on its own scope and begin responding to events
    @OptIn(FlowPreview::class)
    private fun subscribe(synchronizer: SdkSynchronizer) {
        // Note: If any of these callback functions directly touch the UI, then the scope used here
        //       should not live longer than that UI or else the context and view tree will be
        //       invalid and lead to crashes. For now, we use a scope that is cancelled whenever
        //       synchronizer.stop is called.
        //       If the scope of the view is required for one of these, then consider using the
        //       related viewModelScope instead of the synchronizer's scope.
        //       synchronizer.coroutineScope cannot be accessed until the synchronizer is started
        val scope = synchronizer.coroutineScope
        synchronizer.allTransactions.collectWith(scope, transactionsProvider::onTransactions)
        synchronizer.status.collectWith(scope, ::onStatus)
        synchronizer.progress.collectWith(scope, ::onDownloadProgress)
        synchronizer.walletBalances.collectWith(scope, ::onBalance)
        synchronizer.processorInfo.collectWith(scope, ::onProcessorInfo)
    }

    private fun onProcessorError(error: Throwable?): Boolean {
        error?.printStackTrace()
        return true
    }

    private fun onChainError(errorHeight: BlockHeight, rewindHeight: BlockHeight) {
    }

    private fun onStatus(status: Synchronizer.Status) {
        syncState = when (status) {
            Synchronizer.Status.STOPPED -> AdapterState.NotSynced(Exception("stopped"))
            Synchronizer.Status.DISCONNECTED -> AdapterState.NotSynced(Exception("disconnected"))
            Synchronizer.Status.SYNCING -> AdapterState.Syncing()
            Synchronizer.Status.SYNCED -> AdapterState.Synced
            else -> syncState
        }
    }

    private fun onDownloadProgress(progress: PercentDecimal) {
        syncState = AdapterState.Syncing(progress.toPercentage())
    }

    private fun onProcessorInfo(processorInfo: CompactBlockProcessor.ProcessorInfo) {
        syncState = AdapterState.Syncing()
        lastBlockUpdatedSubject.onNext(Unit)
    }

    private fun onBalance(balance: Map<AccountUuid, AccountBalance>?) {
        balance?.get(zcashAccount?.accountUuid)?.sapling?.let {
            balanceUpdatedSubject.onNext(Unit)
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
                to = null,
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
                to = transaction.toAddress,
                from = null,
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

    private fun isValidTransparentAddress(address: String): Boolean {
        val transparentPattern = Pattern.compile("^t[0-9a-zA-Z]{34}$")
        return transparentPattern.matcher(address).matches()
    }

    private fun isValidShieldedAddress(address: String): Boolean {
        val shieldedPattern = Pattern.compile("^z[0-9a-zA-Z]{77}$")
        return shieldedPattern.matcher(address).matches()
    }

    private fun isValidZcashAddress(address: String): Boolean {
        return isValidTransparentAddress(address) || isValidShieldedAddress(address)
    }
}
