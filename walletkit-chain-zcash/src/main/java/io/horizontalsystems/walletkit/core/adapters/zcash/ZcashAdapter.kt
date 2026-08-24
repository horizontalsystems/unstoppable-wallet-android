package io.horizontalsystems.walletkit.core.adapters.zcash

import android.content.Context
import android.util.Log
import cash.z.ecc.android.sdk.CloseableSynchronizer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.fromHex
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountBalance
import cash.z.ecc.android.sdk.model.AccountCreateSetup
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.Proposal
import cash.z.ecc.android.sdk.model.TransactionSubmitResult
import cash.z.ecc.android.sdk.model.UnifiedAddressRequest
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.Zip32AccountIndex
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.AddressType
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.adapters.zcash.ISendZcashAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.UnsupportedAccountException
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.zcash.ZcashShieldingTransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import io.horizontalsystems.walletkit.core.toRawHexString
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.util.Base64
import java.util.Date
import java.util.regex.Pattern
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import io.horizontalsystems.walletkit.entities.Account as WalletAccount

class ZcashAdapter(
    context: Context,
    private val wallet: Wallet,
    restoreSettings: RestoreSettings,
    private val localStorage: ILocalStorage,
    lightWalletEndpoint: LightWalletEndpoint,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendZcashAdapter {

    private val accountBirthday: Long?
    private val existingWallet = localStorage.zcashAccountIds.contains(wallet.account.id)
    private val confirmationsThreshold = 10
    private val decimalCount = 8
    private val network: ZcashNetwork = ZcashNetwork.Mainnet
    private val feeChangeHeight: Long = 1_077_550
    private val ironwoodActivationHeight: Long = when (network) {
        ZcashNetwork.Testnet -> 4_134_000
        else -> 3_428_143 // NU6.3 mainnet activation
    }

    private val appContext: Context = context.applicationContext
    private var migrationProposal: Proposal? = null

    private val synchronizer: CloseableSynchronizer
    private val transactionsProvider: ZcashTransactionsProvider

    private val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val accountType = (wallet.account.type as? AccountType.Mnemonic) ?: throw UnsupportedAccountException()
    private val seed = accountType.seed

    private val zcashAccount: Account

    override val receiveAddress: String

    override val receiveAddressTransparent: String

    override val isMainNet: Boolean = true

    private var currentSyncProgress: Float = 0f
    private var lastProcessorError: Throwable? = null

    private var syncState: AdapterState = AdapterState.Syncing()
        set(value) {
            if (value != field) {
                field = value
                adapterStateUpdatedSubject.onNext(Unit)
            }
        }

    override val debugInfo: String
        get() = ""

    override val balanceState: AdapterState
        get() = syncState

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override var balanceData: BalanceData? = null

    private var accountBalance: AccountBalance? = null

    /**
     * Orchard-pool balance that must be migrated to the Ironwood pool, or null while
     * the network has not reached NU6.3 activation or there is nothing to migrate.
     */
    val ironwoodMigrationRequiredBalance: BigDecimal?
        get() {
            // Only report when fully synced: mid-sync the Orchard balance is provisional
            // (notes spent in not-yet-scanned blocks still count), so the migration
            // alerts would show with a wrong amount
            if (syncState != AdapterState.Synced) return null
            val tip = synchronizer.latestHeight?.value ?: return null
            if (tip < ironwoodActivationHeight) return null
            val orchard = accountBalance?.orchard ?: return null
            // Spendable balance only, matching what proposeIronwoodMigration can sweep:
            // the all-or-nothing proposal fails while any Orchard note is still pending,
            // and showing pending funds here would advertise an amount the confirmation
            // screen cannot migrate
            if (orchard.available.value <= 0) return null
            return orchard.available.convertZatoshiToZec(decimalCount)
        }

    val statusInfo: Map<String, Any>
        get() {
            val statusInfo = LinkedHashMap<String, Any>()
            statusInfo["Last Block Info"] = lastBlockInfo ?: ""
            statusInfo["Sync State"] = syncState
            statusInfo["Birthday Height"] = accountBirthday ?: 0
            return statusInfo
        }

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

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

        accountBirthday = birthday?.value

        synchronizer = Synchronizer.newBlocking(
            context = context,
            zcashNetwork = network,
            alias = getValidAliasFromAccountId(wallet.account.id),
            lightWalletEndpoint = lightWalletEndpoint,
            setup = AccountCreateSetup(accountName = wallet.account.name, keySource = null, seed = FirstClassByteArray(seed)),
            birthday = birthday,
            walletInitMode = walletInitMode,
            isTorEnabled = false,
            isExchangeRateEnabled = false
        )

        zcashAccount = runBlocking { synchronizer.getAccounts().first() }
        receiveAddress = runBlocking { synchronizer.getUnifiedAddress(zcashAccount) }
        receiveAddressTransparent = runBlocking { synchronizer.getTransparentAddress(zcashAccount) }
        transactionsProvider = ZcashTransactionsProvider(zcashAccount.accountUuid, synchronizer as SdkSynchronizer) { txHash ->
            // Migration txids recorded at broadcast; match both hash orientations since
            // TransferResult.Success txid endianness is not guaranteed
            val migrationTxIds = localStorage.zcashMigrationTransactionIds
            migrationTxIds.contains(txHash.toRawHexString()) || migrationTxIds.contains(txHash.toReversedHex())
        }
        synchronizer.onCriticalErrorHandler = { error ->
            Log.e("ZcashAdapter", "Critical error", error)
            true
        }
        synchronizer.onSetupErrorHandler = { error ->
            Log.e("ZcashAdapter", "Setup error", error)
            true
        }


        synchronizer.onProcessorErrorHandler = ::onProcessorError
        synchronizer.onProcessorErrorResolved = {
            lastProcessorError = null
            Log.e("ZcashAdapter", "Processor error resolved")
        }
        synchronizer.onChainErrorHandler = ::onChainError

    }

    override fun start() {
        subscribe(synchronizer as SdkSynchronizer)
        if (!existingWallet) {
            localStorage.zcashAccountIds += wallet.account.id
        }
    }

    override fun stop() {
        synchronizer.close()
    }

    override fun refresh() {
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
        return transactionsProvider.getTransactions(fromParams, transactionType, address, limit)
            .map { getTransactionRecord(it) }
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        return transactionsProvider.getNewTransactionsFlowable(transactionType, address)
            .asFlow()
            .map { transactions ->
                transactions.map { getTransactionRecord(it) }
            }
    }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/zcash/transaction/$transactionHash"

    override val availableBalance: BigDecimal
        get() = balanceData?.available ?: BigDecimal.ZERO

    override suspend fun validate(address: String): ZCashAddressType {
        if (address == receiveAddress) throw ZcashError.SendToSelfNotAllowed
        return when (synchronizer.validateAddress(address)) {
            is AddressType.Invalid -> throw ZcashError.InvalidAddress
            is AddressType.Transparent -> ZCashAddressType.Transparent
            is AddressType.Shielded -> ZCashAddressType.Shielded
            is AddressType.Tex -> ZCashAddressType.Transparent
            AddressType.Unified -> ZCashAddressType.Unified
        }
    }

    /**
     * Gets a fresh unified address with Orchard and Sapling receivers (shielded only).
     * Falls back to the standard unified address if custom address generation fails.
     */
    override suspend fun getFreshReceiveAddress(): String {
        return try {
            synchronizer.getCustomUnifiedAddress(zcashAccount, UnifiedAddressRequest.shielded)
        } catch (_: Exception) {
            receiveAddress
        }
    }

    /**
     * Generates and returns an ephemeral transparent address for one-time use.
     * Used for receiving swaps from decentralized exchanges or other single-use scenarios.
     * Falls back to the standard transparent address if ephemeral address generation fails.
     */
    override suspend fun getFreshReceiveAddressTransparent(): String {
        //TODO transparent address rotation is disabled for now
        return receiveAddressTransparent
//        return try {
//            synchronizer.getSingleUseTransparentAddress(zcashAccount.accountUuid).address
//        } catch (e: Exception) {
//            receiveAddressTransparent
//        }
    }

    /**
     * Proposes an immediate full-balance Orchard -> Ironwood migration and returns the
     * net migrated amount plus the implied fee (Orchard total minus migrated amount).
     * The proposed schedule is retained for the subsequent execution step.
     */
    /**
     * Proposes the Orchard -> Ironwood migration through the SDK: a single all-or-nothing
     * transaction sweeping the account's entire Orchard balance to its own internal
     * receiver, with the fee computed so nothing is left in Orchard. The proposal is
     * retained for [executeIronwoodMigration].
     */
    suspend fun proposeIronwoodMigration(): IronwoodMigrationProposal {
        val orchard = accountBalance?.orchard ?: throw IllegalStateException("Orchard balance is not loaded yet")
        val availableZatoshi = orchard.available.value
        if (availableZatoshi <= 0) {
            throw IllegalStateException("No spendable Orchard balance yet, wait for the wallet to finish syncing")
        }

        val proposal = synchronizer.proposeOrchardToIronwoodMigration(zcashAccount)
        migrationProposal = proposal

        val feeZatoshi = proposal.totalFeeRequired().value
        return IronwoodMigrationProposal(
            amount = Zatoshi((availableZatoshi - feeZatoshi).coerceAtLeast(0)).convertZatoshiToZec(decimalCount),
            fee = Zatoshi(feeZatoshi).convertZatoshiToZec(decimalCount)
        )
    }

    data class IronwoodMigrationProposal(
        val amount: BigDecimal,
        val fee: BigDecimal
    )

    /**
     * Signs and broadcasts the self-transfer proposal retained by [proposeIronwoodMigration]
     * through the ordinary send path, records every submitted txid for transaction-history
     * labeling, and returns the first one.
     */
    suspend fun executeIronwoodMigration(): String {
        val proposal = migrationProposal ?: throw IllegalStateException("Migration was not proposed")

        val txHashes = send(proposal)
        val txHash = txHashes.firstOrNull() ?: throw IllegalStateException("Migration send returned no transaction id")

        migrationProposal = null
        localStorage.zcashMigrationTransactionIds += txHashes.map { it.lowercase() }
        return txHash
    }

    suspend fun sendShieldProposal() {
        val shieldProposal = shieldProposal() ?: throw IllegalStateException("Couldn't create shield proposal")
        send(shieldProposal)
    }

    suspend fun shieldTransactionFee(): BigDecimal? =
        shieldProposal()?.totalFeeRequired()?.convertZatoshiToZec()

    private suspend fun shieldProposal(): Proposal? = synchronizer.proposeShielding(
        account = zcashAccount,
        shieldingThreshold = minimalShieldThreshold.convertZecToZatoshi(),
        memo = ""
    )

    override suspend fun proposeTransfer(amount: BigDecimal, address: String, memo: String): Proposal {
        return transferProposal(amount, address, memo)
    }

    override suspend fun fee(amount: BigDecimal, address: String, memo: String): BigDecimal {
        return transferProposal(amount, address, memo).totalFeeRequired().convertZatoshiToZec(decimalCount)
    }

    private suspend fun transferProposal(
        amount: BigDecimal,
        address: String,
        memo: String
    ) = synchronizer.proposeTransfer(
        account = zcashAccount,
        recipient = address,
        amount = amount.convertZecToZatoshi(),
        memo = memo
    )

    private suspend fun send(proposal: Proposal): List<String> {
        val spendingKey = DerivationTool.getInstance().deriveUnifiedSpendingKey(seed, network, Zip32AccountIndex.new(0))

        try {
            val results = synchronizer.createProposedTransactions(proposal, spendingKey).toList()
            val txHashes = mutableListOf<String>()
            results.forEach { result ->
                when (result) {
                    is TransactionSubmitResult.Success -> {
                        txHashes.add(result.txIdString())
                    }

                    is TransactionSubmitResult.Failure -> {
                        val errorMsg = buildString {
                            append("Transaction submission failed. ")
                            append("TxId: ${result.txIdString()}, ")
                            append("gRPC error: ${result.grpcError}, ")
                            append("Code: ${result.code}, ")
                            append("Description: ${result.description ?: "None"}")
                        }
                        throw IllegalStateException(errorMsg)
                    }

                    is TransactionSubmitResult.NotAttempted -> {
                        throw IllegalStateException("Transaction not attempted. TxId: ${result.txIdString()}")
                    }
                }
            }
            return txHashes
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid proposal: ${e.message}", e)
        } catch (e: Exception) {
            throw RuntimeException("Unexpected error while sending Zcash: ${e.message}", e)
        }
    }

    suspend fun createProposal(outputs: List<TransferOutput>): Proposal {
        val paymentUri = createPaymentUri(outputs)
        return synchronizer.proposeFulfillingPaymentUri(
            zcashAccount,
            paymentUri,
        )
    }

    override suspend fun sendProposal(proposal: Proposal): String? {
        return send(proposal).firstOrNull()
    }

    private fun createPaymentUri(outputs: List<TransferOutput>): String {
        val queryParams = mutableListOf<String>()

        outputs.forEachIndexed { index, output ->
            if (index == 0) {
                queryParams.add("address=${output.address}")
                queryParams.add("amount=${output.amount.toPlainString()}")
                if (output.memo.isNotEmpty()) {
                    queryParams.add("memo=${encodeBase64Url(output.memo)}")
                }
            } else {
                queryParams.add("address.$index=${output.address}")
                queryParams.add("amount.$index=${output.amount.toPlainString()}")
                if (output.memo.isNotEmpty()) {
                    queryParams.add("memo.$index=${encodeBase64Url(output.memo)}")
                }
            }
        }

        return "zcash:?${queryParams.joinToString("&")}"
    }

    private fun encodeBase64Url(string: String): String {
        val encoded = Base64.getEncoder().encodeToString(string.toByteArray(Charsets.UTF_8))
        return encoded
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
    }

    data class TransferOutput(
        val amount: BigDecimal,
        val address: String,
        val memo: String = ""
    )

    // Subscribe to a synchronizer on its own scope and begin responding to events
    private fun subscribe(synchronizer: SdkSynchronizer) {
        // Note: If any of these callback functions directly touch the UI, then the scope used here
        //       should not live longer than that UI or else the context and view tree will be
        //       invalid and lead to crashes. For now, we use a scope that is cancelled whenever
        //       synchronizer.stop is called.
        //       If the scope of the view is required for one of these, then consider using the
        //       related viewModelScope instead of the synchronizer's scope.
        //       synchronizer.coroutineScope cannot be accessed until the synchronizer is started
        val scope = synchronizer.coroutineScope
        scope.launch {
            synchronizer.allTransactions.collect(transactionsProvider::onTransactions)
        }
        scope.launch {
            synchronizer.status.collect(::onStatus)
        }
        scope.launch {
            synchronizer.progress.collect(::onDownloadProgress)
        }
        scope.launch {
            synchronizer.walletBalances.mapNotNull { it?.get(zcashAccount.accountUuid) }.collect(::onBalance)
        }
        scope.launch {
            synchronizer.processorInfo.collect(::onProcessorInfo)
        }
    }

    private fun onProcessorError(error: Throwable?): Boolean {
        Log.e("ZcashAdapter", "Processor error", error)
        lastProcessorError = error
        return true
    }

    private fun onChainError(errorHeight: BlockHeight, rewindHeight: BlockHeight) {

        Log.e("ZcashAdapter", "Chain error errorHeight = $errorHeight, rewindHeight = $rewindHeight")
    }

    private fun onStatus(status: Synchronizer.Status) {
        syncState = when (status) {
            // STOPPED is terminal: the CompactBlockProcessor loop has exited (5 consecutive
            // block-processing failures land here). Mapping it to Syncing showed an endless
            // spinner and hid the stall from the failover gate; only an adapter rebuild (fresh
            // synchronizer) recovers, which reporting NotSynced lets the stall watcher trigger.
            Synchronizer.Status.STOPPED -> AdapterState.NotSynced(lastProcessorError ?: Exception("Sync stopped"))
            Synchronizer.Status.DISCONNECTED -> AdapterState.NotSynced(lastProcessorError ?: Exception("Disconnected"))
            Synchronizer.Status.SYNCING -> AdapterState.Syncing()
            Synchronizer.Status.SYNCED -> {
                lastProcessorError = null
                AdapterState.Synced
            }
            Synchronizer.Status.INITIALIZING -> AdapterState.Syncing()
        }
    }

    private fun onDownloadProgress(progress: PercentDecimal) {
        currentSyncProgress = progress.decimal
        val blocksRemaining = calculateBlocksRemaining()
        val progressPercent = progress.toPercentage().coerceIn(0, 100)

        if (blocksRemaining == null) return

        syncState = AdapterState.Syncing(
            progress = progressPercent,
            blocksRemained = blocksRemaining
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onProcessorInfo(processorInfo: CompactBlockProcessor.ProcessorInfo) {
        lastBlockUpdatedSubject.onNext(Unit)
    }

    private fun calculateBlocksRemaining(): Long? {
        val birthday = accountBirthday ?: return null
        val latestHeight = synchronizer.latestHeight?.value ?: return null
        val totalBlocks = latestHeight - birthday
        if (totalBlocks <= 0 || currentSyncProgress <= 0f) return null

        return max(1L, (totalBlocks * (1 - currentSyncProgress)).toLong())
    }

    private fun onBalance(balance: AccountBalance) {
        accountBalance = balance
        val balanceAvailable = balance.available.convertZatoshiToZec(decimalCount)
        val balancePending = balance.pending.convertZatoshiToZec(decimalCount)
        val balanceUnshielded = balance.unshielded.convertZatoshiToZec(decimalCount)

        balanceData = BalanceData(
            available = balanceAvailable,
            pending = balancePending,
            unshielded = balanceUnshielded
        )

        balanceUpdatedSubject.onNext(Unit)
    }

    private fun getTransactionRecord(transaction: ZcashTransaction): TransactionRecord {
        val transactionHashHex = transaction.transactionHash.toReversedHex()

        return when {
            transaction.shieldDirection != null -> {
                ZcashShieldingTransactionRecord(
                    token = wallet.token,
                    uid = transactionHashHex,
                    transactionHash = transactionHashHex,
                    transactionIndex = transaction.transactionIndex,
                    blockHeight = transaction.minedHeight?.toInt(),
                    confirmationsThreshold = confirmationsThreshold,
                    timestamp = transaction.timestamp,
                    fee = transaction.feePaid?.convertZatoshiToZec(decimalCount),
                    failed = transaction.failed,
                    lockInfo = null,
                    conflictingHash = null,
                    showRawTransaction = false,
                    amount = transaction.value.convertZatoshiToZec(decimalCount),
                    direction = ZcashShieldingTransactionRecord.Direction.from(transaction.shieldDirection),
                    memo = transaction.memo,
                    source = wallet.transactionSource
                )
            }

            transaction.isIncoming -> {
                BitcoinIncomingTransactionRecord(
                    token = wallet.token,
                    uid = transactionHashHex,
                    transactionHash = transactionHashHex,
                    transactionIndex = transaction.transactionIndex,
                    blockHeight = transaction.minedHeight?.toInt(),
                    confirmationsThreshold = confirmationsThreshold,
                    timestamp = transaction.timestamp,
                    fee = transaction.feePaid.convertZatoshiToZec(decimalCount),
                    failed = transaction.failed,
                    lockInfo = null,
                    conflictingHash = null,
                    showRawTransaction = false,
                    amount = transaction.value.convertZatoshiToZec(decimalCount),
                    from = null,
                    memo = transaction.memo,
                    source = wallet.transactionSource
                )
            }

            else -> {
                BitcoinOutgoingTransactionRecord(
                    token = wallet.token,
                    uid = transactionHashHex,
                    transactionHash = transactionHashHex,
                    transactionIndex = transaction.transactionIndex,
                    blockHeight = transaction.minedHeight?.toInt(),
                    confirmationsThreshold = confirmationsThreshold,
                    timestamp = transaction.timestamp,
                    fee = transaction.feePaid.convertZatoshiToZec(decimalCount),
                    failed = transaction.failed,
                    lockInfo = null,
                    conflictingHash = null,
                    showRawTransaction = false,
                    amount = transaction.value.convertZatoshiToZec(decimalCount).negate(),
                    to = transaction.recipients?.firstOrNull()?.addressValue,
                    sentToSelf = transaction.sentToSelf,
                    memo = transaction.memo,
                    source = wallet.transactionSource,
                    replaceable = false
                )
            }
        }
    }

    enum class ZCashAddressType {
        Shielded, Transparent, Unified
    }

    sealed class ZcashError : Exception() {
        object InvalidAddress : ZcashError()
        object SendToSelfNotAllowed : ZcashError()
    }

    companion object {
        val minimalShieldThreshold = BigDecimal("0.0004") // minimal transparent balance to shielding

        private const val ALIAS_PREFIX = "zcash_"

        private fun getValidAliasFromAccountId(accountId: String): String {
            return ALIAS_PREFIX + accountId.replace("-", "_")
        }

        fun clear(accountId: String) {
            runBlocking {
                Synchronizer.erase(App.instance, ZcashNetwork.Mainnet, getValidAliasFromAccountId(accountId))
            }
        }

        suspend fun getTransparentAddress(account: WalletAccount, lightWalletEndpoint: LightWalletEndpoint): String =
            withTemporarySynchronizer(account, lightWalletEndpoint) { synchronizer ->
                synchronizer.getTransparentAddress(synchronizer.getAccounts().first())
            }

        suspend fun getUnifiedAddress(account: WalletAccount, lightWalletEndpoint: LightWalletEndpoint): String =
            withTemporarySynchronizer(account, lightWalletEndpoint) { synchronizer ->
                synchronizer.getUnifiedAddress(synchronizer.getAccounts().first())
            }

        private suspend fun <T> withTemporarySynchronizer(
            account: WalletAccount,
            lightWalletEndpoint: LightWalletEndpoint,
            block: suspend (Synchronizer) -> T,
        ): T {
            val seed = (account.type as? AccountType.Mnemonic)?.seed
                ?: throw IllegalArgumentException("Unsupported account type for Zcash")

            val alias = getValidAliasFromAccountId(account.id)
            val network = ZcashNetwork.Mainnet
            val context = App.instance
            val existingWallet = App.localStorage.zcashAccountIds.contains(account.id)
            val restoreSettings = App.restoreSettingsManager.settings(account, BlockchainType.Zcash)

            val walletInitMode = if (existingWallet) {
                WalletInitMode.ExistingWallet
            } else when (account.origin) {
                AccountOrigin.Created -> WalletInitMode.NewWallet
                AccountOrigin.Restored -> WalletInitMode.RestoreWallet
            }

            val birthday = when (account.origin) {
                AccountOrigin.Created -> {
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

            val synchronizer = Synchronizer.newBlocking(
                context = context,
                zcashNetwork = network,
                alias = alias,
                lightWalletEndpoint = lightWalletEndpoint,
                setup = AccountCreateSetup(
                    accountName = account.name,
                    keySource = null,
                    seed = FirstClassByteArray(seed)
                ),
                birthday = birthday,
                walletInitMode = walletInitMode,
                isTorEnabled = false,
                isExchangeRateEnabled = false
            )

            return synchronizer.use { synchronizer ->
                block(synchronizer)
            }
        }

        suspend fun estimateBirthdayHeight(context: Context, date: Date): Long {
            val blockHeight = SdkSynchronizer.estimateBirthdayHeight(
                context = context,
                date = Instant.fromEpochMilliseconds(date.time),
                network = ZcashNetwork.Mainnet
            )
            return blockHeight.value
        }

        suspend fun estimateBirthdayDate(context: Context, height: Long) = try {
            val instant = SdkSynchronizer.estimateBirthdayDate(
                context = context,
                height = BlockHeight.new(height),
                network = ZcashNetwork.Mainnet
            )

            Date(instant.toEpochMilliseconds())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
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

val AccountBalance.available: Zatoshi
    get() = this.sapling.available + this.orchard.available + this.ironwood.available

val AccountBalance.pending: Zatoshi
    get() = this.sapling.pending + this.orchard.pending + this.ironwood.pending
