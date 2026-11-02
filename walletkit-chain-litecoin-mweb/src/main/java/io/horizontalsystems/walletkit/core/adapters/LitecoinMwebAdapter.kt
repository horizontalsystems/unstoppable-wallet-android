package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.litecoinkit.LitecoinKit
import io.horizontalsystems.litecoinmwebkit.LitecoinMwebKit
import io.horizontalsystems.litecoinmwebkit.MwebBalance
import io.horizontalsystems.litecoinmwebkit.MwebFee
import io.horizontalsystems.litecoinmwebkit.MwebSendError
import io.horizontalsystems.litecoinmwebkit.MwebSyncState
import io.horizontalsystems.litecoinmwebkit.MwebTransactionInfo
import io.horizontalsystems.litecoinmwebkit.MwebTransactionStatus
import io.horizontalsystems.litecoinmwebkit.MwebTransactionType
import io.horizontalsystems.litecoinmwebkit.TemplateOutput
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.IPegInMwebAdapter
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.ISendMwebAdapter
import io.horizontalsystems.walletkit.core.ITorManager
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.LocalizedException
import io.horizontalsystems.walletkit.core.UnsupportedAccountException
import io.horizontalsystems.walletkit.core.UsedAddress
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.mweb.MwebIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.mweb.MwebOutgoingTransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.math.min

/**
 * Wraps one [LitecoinMwebKit]. History comes from the kit's own table, written as events
 * happen: the light client cannot replay spent outputs, so a restore shows the unspent
 * incoming outputs only.
 */
class LitecoinMwebAdapter(
    private val kit: LitecoinMwebKit,
    private val wallet: Wallet,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendMwebAdapter, IPegInMwebAdapter, IStatusInfoAdapter {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val token: Token get() = wallet.token
    private val decimals = wallet.token.decimals

    private val _balanceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _stateUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _lastBlockUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _changedTransactionsFlow = MutableSharedFlow<List<MwebTransactionInfo>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var transactions: List<MwebTransactionInfo> = emptyList()
    // The kit reads its balance from Room, which refuses the main thread; the balance getter is
    // called from there, so it serves the last value the kit's flow delivered.
    @Volatile
    private var balance = MwebBalance(0, 0)
    // The next unused address also comes from Room; it changes when an output or a pending
    // peg-in uses the current one, so it is recomputed with the balance and the history.
    @Volatile
    private var nextReceiveAddress: String? = null
    private var knownTransactions: Map<String, Pair<MwebTransactionStatus, Int?>> = emptyMap()

    // region IAdapter

    override fun start() {
        scope.launch {
            kit.syncStateFlow.collect {
                balanceState = it.toAdapterState()
                _stateUpdatedFlow.tryEmit(Unit)
            }
        }
        scope.launch {
            kit.balanceFlow.collect {
                balance = it
                nextReceiveAddress = kit.receiveAddress()
                _balanceUpdatedFlow.tryEmit(Unit)
            }
        }
        scope.launch {
            kit.lastBlockHeightFlow.collect {
                _lastBlockUpdatedFlow.tryEmit(Unit)
                // Confirmations change the spendable part of the balance.
                _balanceUpdatedFlow.tryEmit(Unit)
            }
        }
        scope.launch {
            kit.transactionsFlow.collect {
                onTransactions(it)
                nextReceiveAddress = kit.receiveAddress()
            }
        }
        kit.start()
    }

    override fun stop() {
        kit.stop()
        scope.cancel()
    }

    override fun refresh() {
        if (kit.syncState is MwebSyncState.NotSynced) {
            kit.refresh()
        }
    }

    override val debugInfo: String = ""

    override val statusInfo: Map<String, Any>
        get() = mapOf(
            "Sync State" to kit.syncState.toString(),
            "Last Block Height" to kit.lastBlockHeightFlow.value,
            "Balance" to balance.toString(),
        )

    // endregion

    // region IBalanceAdapter

    override var balanceState: AdapterState = kit.syncState.toAdapterState()
        private set

    override val balanceStateUpdatedFlow: Flow<Unit> get() = _stateUpdatedFlow

    override val balanceData: BalanceData
        get() = BalanceData(available = litoshiToLtc(balance.spendable), pending = litoshiToLtc(balance.unconfirmed))

    override val balanceUpdatedFlow: Flow<Unit> get() = _balanceUpdatedFlow

    // endregion

    // region IReceiveAdapter

    override val receiveAddress: String get() = nextReceiveAddress ?: kit.receiveAddress()

    override val isMainNet: Boolean = true

    // No public explorer can show an MWEB address, so the rows carry no link.
    override fun usedAddresses(change: Boolean): List<UsedAddress> =
        if (change) emptyList() else kit.usedAddresses().map { (index, address) -> UsedAddress(index, address, "") }

    // endregion

    // region ITransactionsAdapter

    override val explorerTitle: String = "blockchair.com"

    override val transactionsState: AdapterState get() = balanceState

    override val transactionsStateUpdatedFlow: Flow<Unit> get() = _stateUpdatedFlow

    override val lastBlockInfo: LastBlockInfo?
        get() = kit.lastBlockHeightFlow.value.takeIf { it > 0 }?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlow: Flow<Unit> get() = _lastBlockUpdatedFlow

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> {
        if (address != null) return emptyList()
        val filtered = transactions.filter { matches(it, transactionType) }
        val fromIndex = from?.let { record -> filtered.indexOfFirst { it.uid == record.uid } + 1 } ?: 0
        if (fromIndex >= filtered.size) return emptyList()
        return filtered.subList(fromIndex, min(filtered.size, fromIndex + limit)).map { record(it) }
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        if (address != null) return emptyFlow()
        return _changedTransactionsFlow
            .map { changed -> changed.filter { matches(it, transactionType) }.map { record(it) } }
            .filter { it.isNotEmpty() }
    }

    // Only peg-ins and peg-outs have a Litecoin transaction; for the others the hash is the
    // record uid and this link leads nowhere, which the info screen must take into account.
    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/litecoin/transaction/$transactionHash"

    private fun onTransactions(list: List<MwebTransactionInfo>) {
        val changed = list.filter { knownTransactions[it.uid] != (it.status to it.blockHeight) }
        transactions = list
        knownTransactions = list.associate { it.uid to (it.status to it.blockHeight) }
        if (changed.isNotEmpty()) {
            _changedTransactionsFlow.tryEmit(changed)
        }
    }

    private fun matches(tx: MwebTransactionInfo, filter: FilterTransactionType): Boolean = when (filter) {
        FilterTransactionType.All -> true
        FilterTransactionType.Incoming -> tx.type == MwebTransactionType.Incoming || tx.type == MwebTransactionType.PegIn
        FilterTransactionType.Outgoing -> tx.type == MwebTransactionType.Outgoing ||
                tx.type == MwebTransactionType.PegOut ||
                tx.type == MwebTransactionType.SpentElsewhere
        FilterTransactionType.Swap,
        FilterTransactionType.Approve -> false
    }

    private fun record(tx: MwebTransactionInfo): TransactionRecord {
        val amount = litoshiToLtc(tx.amount)
        val fee = tx.fee?.let { litoshiToLtc(it) }
        val failed = tx.status == MwebTransactionStatus.Failed
        val hash = tx.txHash ?: tx.uid

        return when (tx.type) {
            MwebTransactionType.Incoming,
            MwebTransactionType.PegIn -> MwebIncomingTransactionRecord(
                token = token,
                uid = tx.uid,
                transactionHash = hash,
                blockHeight = tx.blockHeight,
                confirmationsThreshold = CONFIRMATIONS_THRESHOLD,
                timestamp = tx.timestamp,
                fee = fee,
                failed = failed,
                amount = amount,
                pegIn = tx.type == MwebTransactionType.PegIn,
                litecoinTxHash = tx.txHash,
                source = wallet.transactionSource,
            )

            MwebTransactionType.Outgoing,
            MwebTransactionType.PegOut,
            MwebTransactionType.SpentElsewhere -> MwebOutgoingTransactionRecord(
                token = token,
                uid = tx.uid,
                transactionHash = hash,
                blockHeight = tx.blockHeight,
                confirmationsThreshold = CONFIRMATIONS_THRESHOLD,
                timestamp = tx.timestamp,
                fee = fee,
                failed = failed,
                amount = amount.negate(),
                to = tx.toAddress,
                pegOut = tx.type == MwebTransactionType.PegOut,
                spentElsewhere = tx.type == MwebTransactionType.SpentElsewhere,
                litecoinTxHash = tx.txHash,
                source = wallet.transactionSource,
            )
        }
    }

    // endregion

    // region ISendMwebAdapter

    override fun validate(address: String) {
        kit.validateDestination(address)
    }

    // The kit's coin selection and sends read and write its Room database, so they run on IO.

    override suspend fun estimateFee(amount: BigDecimal, address: String, feeRate: Int): BigDecimal = withContext(Dispatchers.IO) {
        litoshiToLtc(kit.estimateFee(address, ltcToLitoshi(amount), feeRate))
    }

    override suspend fun maximumSendable(address: String, feeRate: Int): BigDecimal = withContext(Dispatchers.IO) {
        litoshiToLtc(kit.maximumSpendableValue(address, feeRate))
    }

    override suspend fun send(amount: BigDecimal, address: String, feeRate: Int): String = withContext(Dispatchers.IO) {
        broadcastUnknownAsLocalized {
            kit.send(address, ltcToLitoshi(amount), feeRate).uid
        }
    }

    // endregion

    // region IPegInMwebAdapter

    // The fee depends only on the output being an MWEB one, so any 66-byte key script stands in
    // for the real address and no database read is needed.
    override fun pegInMwebFee(feeRate: Int): BigDecimal =
        litoshiToLtc(MwebFee.estimate(listOf(TemplateOutput(ANY_MWEB_SCRIPT, 0)), feeRate.toLong() * 1000))

    override suspend fun estimatePegInFee(litecoinKit: LitecoinKit, amount: BigDecimal, feeRate: Int): BigDecimal = withContext(Dispatchers.IO) {
        litoshiToLtc(kit.estimatePegInFee(litecoinKit, ltcToLitoshi(amount), feeRate).total)
    }

    override suspend fun pegIn(litecoinKit: LitecoinKit, address: String, amount: BigDecimal, feeRate: Int): String = withContext(Dispatchers.IO) {
        broadcastUnknownAsLocalized {
            kit.pegIn(litecoinKit, address, ltcToLitoshi(amount), feeRate).txHash
                ?: throw IllegalStateException("peg-in recorded without a Litecoin transaction hash")
        }
    }

    // endregion

    // The kit keeps the row pending when the daemon's answer was lost; the user must see
    // "unknown", not "failed", so they check the history before paying again.
    private inline fun <T> broadcastUnknownAsLocalized(block: () -> T): T = try {
        block()
    } catch (e: MwebSendError.BroadcastUnknown) {
        throw LocalizedException(R.string.Mweb_BroadcastUnknown)
    }

    private fun litoshiToLtc(value: Long): BigDecimal = BigDecimal.valueOf(value).movePointLeft(decimals)

    private fun ltcToLitoshi(value: BigDecimal): Long = value.movePointRight(decimals).toLong()

    companion object {
        // MWEB outputs are final after one block; the extension block is part of consensus.
        const val CONFIRMATIONS_THRESHOLD = 1

        private val ANY_MWEB_SCRIPT = ByteArray(66).also { it[0] = 2; it[33] = 2 }

        fun create(wallet: Wallet, torManager: ITorManager): LitecoinMwebAdapter {
            val accountType = wallet.account.type as? AccountType.Mnemonic ?: throw UnsupportedAccountException()
            // TorManager publishes the SOCKS port through the JVM proxy properties, which the Go
            // daemon does not read. While Tor is starting the port is the blocked one, so the daemon
            // fails until Tor connects and the plugin's reload trigger rebuilds the adapter.
            val proxyUrl = if (torManager.isTorEnabled) {
                "socks5://127.0.0.1:${System.getProperty("socksProxyPort") ?: "1"}"
            } else {
                null
            }
            val kit = LitecoinMwebKit(
                context = App.instance,
                words = accountType.words,
                passphrase = accountType.passphrase,
                walletId = wallet.account.id,
                networkType = LitecoinKit.NetworkType.MainNet,
                proxyUrl = proxyUrl,
            )
            return LitecoinMwebAdapter(kit, wallet)
        }

        fun clear(walletId: String) {
            LitecoinMwebKit.clear(App.instance, LitecoinKit.NetworkType.MainNet, walletId)
        }
    }
}

fun MwebSyncState.toAdapterState(): AdapterState = when (this) {
    MwebSyncState.Synced -> AdapterState.Synced
    is MwebSyncState.Syncing -> when (stage) {
        MwebSyncState.Stage.Starting -> AdapterState.Connecting
        MwebSyncState.Stage.BlockHeaders,
        MwebSyncState.Stage.MwebData -> AdapterState.Syncing(progress = (progress * 100).toInt().coerceIn(0, 100))
    }
    // The kit reports "not started" and "stopped" as NotSynced; before start they are a gap, not an error.
    is MwebSyncState.NotSynced -> when (exception.message) {
        "not started", "stopped" -> AdapterState.Connecting
        else -> AdapterState.NotSynced(exception)
    }
}
