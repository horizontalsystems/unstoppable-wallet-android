package io.horizontalsystems.bankwallet.core.adapters.zcash

import android.content.Context
import cash.z.ecc.android.sdk.CloseableSynchronizer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.block.processor.CompactBlockProcessor
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.collectWith
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
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.Zip32AccountIndex
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.AddressType
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendZcashAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.zcash.ZcashShieldingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.util.regex.Pattern
import kotlin.math.max

class ZcashAdapter(
    context: Context,
    private val wallet: Wallet,
    restoreSettings: RestoreSettings,
    private val localStorage: ILocalStorage,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendZcashAdapter {

    private val accountBirthday: Long?
    private val existingWallet = localStorage.zcashAccountIds.contains(wallet.account.id)
    private val confirmationsThreshold = 10
    private val decimalCount = 8
    private val network: ZcashNetwork = ZcashNetwork.Mainnet
    private val feeChangeHeight: Long = 1_077_550
    private val lightWalletEndpoint = LightWalletEndpoint(host = "zec.rocks", port = 443, isSecure = true)

    private val synchronizer: CloseableSynchronizer
    private val transactionsProvider: ZcashTransactionsProvider

    private val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val accountType = (wallet.account.type as? AccountType.Mnemonic) ?: throw UnsupportedAccountException()
    private val seed = accountType.seed

    private val zcashAccount: Account

    override val receiveAddress: String

    override val receiveAddressTransparent: String?

    override val isMainNet: Boolean = true

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
        transactionsProvider = ZcashTransactionsProvider(zcashAccount.accountUuid, synchronizer as SdkSynchronizer)
        synchronizer.onProcessorErrorHandler = ::onProcessorError
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

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): Single<List<TransactionRecord>> {
        val fromParams = from?.let {
            val transactionHash = it.transactionHash.fromHex().reversedArray()
            Triple(transactionHash, it.timestamp, it.transactionIndex)
        }
        return transactionsProvider.getTransactions(fromParams, transactionType, address, limit)
            .map { transactions ->
                transactions.map {
                    getTransactionRecord(it)
                }
            }
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flowable<List<TransactionRecord>> {
        return transactionsProvider.getNewTransactionsFlowable(transactionType, address)
            .map { transactions ->
                transactions.map { getTransactionRecord(it) }
            }
    }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/zcash/transaction/$transactionHash"

    override val availableBalance: BigDecimal
        get() = balanceData?.available ?: BigDecimal.ZERO

    override val fee: BigDecimal
        get() = ZcashSdk.MINERS_FEE.convertZatoshiToZec(decimalCount)

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

    override suspend fun send(amount: BigDecimal, address: String, memo: String, logger: AppLogger) {
        logger.info("call sendTransferProposal")
        sendTransferProposal(amount, address, memo)
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

    private suspend fun send(proposal: Proposal) {
        val spendingKey = DerivationTool.getInstance().deriveUnifiedSpendingKey(seed, network, Zip32AccountIndex.new(0))

        try {
            val results = synchronizer.createProposedTransactions(proposal, spendingKey).toList()
            results.forEach { result ->
                when (result) {
                    is TransactionSubmitResult.Success -> {}

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
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid proposal: ${e.message}", e)
        } catch (e: Exception) {
            throw RuntimeException("Unexpected error while sending Zcash: ${e.message}", e)
        }
    }

    private suspend fun sendTransferProposal(
        amount: BigDecimal,
        address: String,
        memo: String
    ) {
        val transferProposal = transferProposal(amount, address, memo)
        send(transferProposal)
    }

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
        synchronizer.allTransactions.collectWith(scope, transactionsProvider::onTransactions)
        synchronizer.status.collectWith(scope, ::onStatus)
        synchronizer.progress.collectWith(scope, ::onDownloadProgress)
        synchronizer.walletBalances.mapNotNull { it?.get(zcashAccount.accountUuid) }.collectWith(scope, ::onBalance)
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
            Synchronizer.Status.INITIALIZING -> AdapterState.Connecting
        }
    }

    private fun onDownloadProgress(progress: PercentDecimal) {
        syncState = AdapterState.Downloading(progress.toPercentage())
    }

    private fun onProcessorInfo(processorInfo: CompactBlockProcessor.ProcessorInfo) {
        syncState = AdapterState.Syncing()
        lastBlockUpdatedSubject.onNext(Unit)
    }

    private fun onBalance(balance: AccountBalance) {
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
                    sentToSelf = false,
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
    get() = this.sapling.available + this.orchard.available

val AccountBalance.pending: Zatoshi
    get() = this.sapling.pending + this.orchard.pending
