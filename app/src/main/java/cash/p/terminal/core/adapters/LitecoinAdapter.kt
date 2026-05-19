package cash.p.terminal.core.adapters

import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.IFeeRateProvider
import cash.p.terminal.core.IMwebAddressValidator
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.TransactionExplorerData
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.derivation
import cash.p.terminal.core.managers.LITECOIN_MWEB_PEG_IN_MATCH_TIMESTAMP_TOLERANCE_SECONDS
import cash.p.terminal.core.managers.LitecoinMwebRestoreHeight
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.purpose
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.core.toRawHexString
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.TransactionDataSortMode
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.entities.UsedAddress
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.network.peer.SharedPeerGroupHolder
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.DefaultDispatcherProvider
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.litecoinkit.LitecoinKit
import io.horizontalsystems.litecoinkit.LitecoinKit.NetworkType
import io.horizontalsystems.litecoinkit.LitecoinMwebState
import io.horizontalsystems.litecoinkit.LitecoinReceiveAddressType
import io.horizontalsystems.litecoinkit.LitecoinSendInfo
import io.horizontalsystems.litecoinkit.LitecoinSendResult
import io.horizontalsystems.litecoinkit.LitecoinSendSource
import io.horizontalsystems.litecoinkit.mweb.CoroutineMwebDispatcherProvider
import io.horizontalsystems.litecoinkit.mweb.MwebBalance
import io.horizontalsystems.litecoinkit.mweb.MwebConfig
import io.horizontalsystems.litecoinkit.mweb.MwebNetworkPolicy
import io.horizontalsystems.litecoinkit.mweb.MwebPublicSendConfig
import io.horizontalsystems.litecoinkit.mweb.MwebRestorePoint
import io.horizontalsystems.litecoinkit.mweb.MwebSendResult
import io.horizontalsystems.litecoinkit.mweb.MwebSyncState
import io.horizontalsystems.litecoinkit.mweb.MwebTransaction
import io.horizontalsystems.litecoinkit.mweb.MwebTransactionKind
import io.horizontalsystems.litecoinkit.mweb.MwebTransactionType
import io.horizontalsystems.litecoinkit.mweb.MwebUtxo
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.security.MessageDigest
import kotlin.math.abs

class LitecoinAdapter(
    override val kit: LitecoinKit,
    syncMode: BitcoinCore.SyncMode,
    backgroundManager: BackgroundManager,
    wallet: Wallet,
    private val mode: Mode,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    feeRateProvider: IFeeRateProvider? = null
) : BitcoinBaseAdapter(kit, syncMode, backgroundManager, wallet, DISPLAY_CONFIRMATIONS_THRESHOLD, feeRateProvider = feeRateProvider), LitecoinKit.Listener, ISendBitcoinAdapter, IMwebAddressValidator {

    sealed interface Mode {
        data class Public(val derivation: TokenType.Derivation) : Mode
        data object Mweb : Mode
    }

    constructor(
        wallet: Wallet,
        syncMode: BitcoinCore.SyncMode,
        backgroundManager: BackgroundManager,
        derivation: TokenType.Derivation,
        mwebRestoreSettings: RestoreSettings? = null,
        dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
        feeRateProvider: IFeeRateProvider? = null
    ) : this(
        createKit(
            wallet = wallet,
            syncMode = syncMode,
            mode = Mode.Public(derivation),
            restoreSettings = mwebRestoreSettings,
            dispatcherProvider = dispatcherProvider
        ),
        syncMode,
        backgroundManager,
        wallet,
        mode = Mode.Public(derivation),
        dispatcherProvider = dispatcherProvider,
        feeRateProvider = feeRateProvider
    )

    constructor(
        wallet: Wallet,
        syncMode: BitcoinCore.SyncMode,
        backgroundManager: BackgroundManager,
        restoreSettings: RestoreSettings,
        dispatcherProvider: DispatcherProvider,
        feeRateProvider: IFeeRateProvider? = null
    ) : this(
        createKit(
            wallet = wallet,
            syncMode = syncMode,
            mode = Mode.Mweb,
            restoreSettings = restoreSettings,
            dispatcherProvider = dispatcherProvider
        ),
        syncMode,
        backgroundManager,
        wallet,
        mode = Mode.Mweb,
        dispatcherProvider = dispatcherProvider,
        feeRateProvider = feeRateProvider
    )

    private val mwebMode: Boolean
        get() = mode == Mode.Mweb

    @Volatile
    private var latestMwebSyncState: MwebSyncState? = null
    @Volatile
    private var stopped = false
    private val mwebAvailableBalanceCache = linkedMapOf<MwebAvailableBalanceKey, BigDecimal>()
    private val mwebFeeInfoCache = linkedMapOf<MwebFeeInfoKey, CachedMwebFeeInfo>()
    private var cachedMwebTransactions: List<MwebTransaction>? = null
    private var cachedMwebTransactionRecords: List<TransactionRecord> = emptyList()
    private val mwebQuoteUpdatedSubject = PublishSubject.create<Unit>()
    private var mwebAvailableBalanceCalculationKey: MwebAvailableBalanceKey? = null
    private var mwebAvailableBalanceCalculationJob: Job? = null
    private var mwebFeeInfoCalculationKey: MwebFeeInfoKey? = null
    private var mwebFeeInfoCalculationJob: Job? = null
    private var cachedMwebAdapterState: AdapterState? = null

    @Volatile
    private var cachedOwnMwebAddress: String? = null
    private var ownMwebAddressFetchJob: Job? = null

    private val currentMwebSyncState: MwebSyncState?
        get() = latestMwebSyncState ?: mwebStateOrNull()?.syncState

    init {
        kit.listener = this
    }

    //
    // LitecoinKit Listener
    //

    override val explorerTitle: String
        get() = BLOCKCHAIR_EXPLORER_TITLE

    override fun stop() {
        stopped = true
        scope.cancel()
        ownMwebAddressFetchJob = null
        kit.dispose()
    }

    override val receiveAddress: String
        get() = if (mwebMode) {
            kit.receiveAddress(LitecoinReceiveAddressType.Mweb)
        } else {
            super.receiveAddress
        }

    override val isAddressHistorySupported: Boolean
        get() = !mwebMode

    override val balanceState: AdapterState
        get() = if (mwebMode) mwebAdapterState() else super.balanceState

    override val balanceUpdatedFlow: Flow<Unit>
        get() {
            return merge(
                super.balanceUpdatedFlow,
                mwebQuoteUpdatedSubject
                    .toFlowable(BackpressureStrategy.BUFFER)
                    .asFlow()
            )
        }

    override val transactionsState: AdapterState
        get() = if (mwebMode) mwebAdapterState() else super.transactionsState

    override val lastBlockInfo: LastBlockInfo?
        @Synchronized
        get() {
            if (!mwebMode) return super.lastBlockInfo

            val mwebHeight = currentMwebSyncState
                ?.mwebUtxosHeight
                ?.takeIf { it > 0 }

            return mwebHeight
                ?.let { LastBlockInfo(it, super.lastBlockInfo?.timestamp) }
                ?: super.lastBlockInfo
        }

    override val balanceData: BalanceData
        get() {
            if (!mwebMode) return super.balanceData

            val balance = kit.litecoinBalance.mweb ?: MwebBalance(confirmed = 0, unconfirmed = 0)
            return BalanceData(
                available = satoshiToBTC(balance.confirmed + balance.unconfirmed),
                timeLocked = BigDecimal.ZERO,
                notRelayed = BigDecimal.ZERO
            )
        }

    override val maxSpendableBalance: BigDecimal
        get() = if (mwebMode) balanceData.available else super.maxSpendableBalance

    override val statusInfo: Map<String, Any>
        get() = if (mwebMode) {
            mwebStateOrNull()?.debugInfo?.let { debugInfo ->
                mapOf(
                    "mwebBlockHeaderHeight" to debugInfo.state.blockHeaderHeight,
                    "mwebHeaderHeight" to debugInfo.state.mwebHeaderHeight,
                    "mwebUtxosHeight" to debugInfo.state.mwebUtxosHeight,
                    "mwebAddressPoolSize" to debugInfo.addressPoolSize,
                    "mwebUnspentUtxoCount" to debugInfo.unspentUtxoCount,
                    "mwebPendingTransactionCount" to debugInfo.pendingTransactionCount,
                    "mwebNativeVersion" to debugInfo.nativeVersion
                )
            } ?: emptyMap()
        } else {
            super.statusInfo
        }

    override fun getTransactionUrl(transactionHash: String): String =
        blockchairTransactionUrl(transactionHash)

    override fun getTransactionExplorerData(record: TransactionRecord): List<TransactionExplorerData> {
        if (!mwebMode || record.token.type != TokenType.Mweb) {
            return super.getTransactionExplorerData(record)
        }

        return buildList {
            val canonicalHash = (record as? BitcoinTransactionRecord)
                ?.canonicalTransactionHash
                ?.takeIf { it.isNotBlank() }
            canonicalHash?.let { hash ->
                add(TransactionExplorerData(BLOCKCHAIR_EXPLORER_TITLE, blockchairTransactionUrl(hash)))
            }
            record.blockHeight?.let { height ->
                add(TransactionExplorerData(MWEB_EXPLORER_TITLE, "$MWEB_EXPLORER_BLOCK_URL$height"))
            }
        }
    }

    override fun onBalanceUpdate(balance: BalanceInfo) {
        if (mwebMode) return

        clearMwebQuoteCache()
        scope.launch {
            estimateFeeForMax()
            balanceUpdatedSubject.onNext(Unit)
        }
    }

    @Synchronized
    override fun onMwebBalanceUpdate(balance: MwebBalance) {
        if (mwebMode) {
            clearMwebQuoteCache()
            scope.launch {
                estimateFeeForMax()
                balanceUpdatedSubject.onNext(Unit)
            }
        }
    }

    override fun onMwebSyncStateUpdate(state: MwebSyncState) {
        val heightChanged = synchronized(this) {
            if (!mwebMode) return

            val previousMwebUtxosHeight = latestMwebSyncState?.mwebUtxosHeight
            latestMwebSyncState = state
            clearMwebAdapterStateCache()
            val heightChanged = previousMwebUtxosHeight != state.mwebUtxosHeight
            if (heightChanged) {
                // Cached dry-run results depend on the chain tip (peg-out maturity, fees);
                // a new tip can change them even when balance/UTXO set is unchanged.
                clearMwebQuoteCache()
            }
            heightChanged
        }

        adapterStateUpdatedSubject.onNext(Unit)
        if (heightChanged) {
            lastBlockUpdatedSubject.onNext(Unit)
            mwebQuoteUpdatedSubject.onNext(Unit)
        }
    }

    override fun onMwebUtxosUpdate(utxos: List<MwebUtxo>) {
        val records = synchronized(this) {
            if (!mwebMode) return

            clearMwebQuoteCache()
            clearMwebTransactionRecordsCache()
            mwebTransactionRecords()
        }
        transactionRecordsSubject.onNext(records)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        clearMwebAdapterStateCache()
        lastBlockUpdatedSubject.onNext(Unit)
    }

    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
        clearMwebAdapterStateCache()
        setState(state)
    }

    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        if (mwebMode) return

        val records = mutableListOf<TransactionRecord>()

        for (info in inserted) {
            records.add(transactionRecord(info))
        }

        for (info in updated) {
            records.add(transactionRecord(info))
        }

        transactionRecordsSubject.onNext(records)
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        // ignored for now
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        if (!mwebMode) {
            if (address != null) return emptyFlow()
            return publicTransactionRecordsFlow(transactionType)
        }
        if (address != null) return emptyFlow()

        return mwebTransactionRecordsFlow(transactionType)
    }

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> {
        if (!mwebMode) {
            return withContext(dispatcherProvider.io) {
                adjustPublicMwebPegInRecords(
                    super.getTransactions(from, token, limit, transactionType, address)
                )
            }
        }
        if (address != null) return emptyList()

        val records = mwebTransactionRecords(transactionType)
        val startIndex = from?.let { record ->
            records.indexOfFirst { it.uid == record.uid }
                .takeIf { it >= 0 }
                ?.plus(1)
        } ?: 0
        return records.drop(startIndex).take(limit)
    }

    private fun adjustPublicMwebPegInRecords(records: List<TransactionRecord>): List<TransactionRecord> {
        if (mwebMode || records.isEmpty()) return records

        val bitcoinRecords = records.filterIsInstance<BitcoinTransactionRecord>()
        if (bitcoinRecords.isEmpty()) return records

        val mwebPegIns = mwebStateOrNull()?.transactions
            ?.filter { it.kind == MwebTransactionKind.PublicToMweb && it.type == MwebTransactionType.Incoming }
            .orEmpty()

        val displayChanges = publicMwebPegInDisplayChanges(bitcoinRecords, mwebPegIns)
        if (displayChanges.isEmpty) return records

        return records.mapNotNull { record ->
            val bitcoinRecord = record as? BitcoinTransactionRecord ?: return@mapNotNull record
            if (bitcoinRecord.uid in displayChanges.hiddenChangeUids) return@mapNotNull null

            displayChanges.adjustedAmounts[bitcoinRecord.uid]?.let(bitcoinRecord::withMainAmount) ?: record
        }
    }

    private fun publicTransactionRecordsFlow(transactionType: FilterTransactionType): Flow<List<TransactionRecord>> {
        val recordsFlow = transactionRecordsSubject
            .toFlowable(BackpressureStrategy.BUFFER)
            .asFlow()
            .map(::adjustPublicMwebPegInRecords)
            .flowOn(dispatcherProvider.io)

        return when (transactionType) {
            FilterTransactionType.All -> recordsFlow
            FilterTransactionType.Incoming,
            FilterTransactionType.Outgoing -> {
                recordsFlow
                    .map { records -> filterBitcoinTransactionRecords(records, transactionType) }
                    .filter { it.isNotEmpty() }
            }
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> {
                emptyFlow()
            }
        }
    }

    private fun publicMwebPegInDisplayChanges(
        records: List<BitcoinTransactionRecord>,
        mwebPegIns: List<MwebTransaction>
    ): PublicMwebPegInDisplayChanges {
        val usedPegInIndexes = mutableSetOf<Int>()
        val hiddenChangeUids = mutableSetOf<String>()
        val adjustedAmounts = mutableMapOf<String, BigDecimal>()

        records
            .filter { it.isPublicMwebPegInMarker() }
            .forEach { record ->
                val matchedPegIn = mwebPegIns.withIndex()
                    .filter { it.index !in usedPegInIndexes }
                    .filter { record.matchesPublicMwebPegIn(it.value) }
                    .minByOrNull { (_, pegIn) -> abs(record.timestamp - pegIn.timestamp) }
                val matchedPegInAmount = matchedPegIn?.value?.let { satoshiToBTC(it.amount).abs() }

                val matchedChange = record.matchPublicMwebChange(records, hiddenChangeUids, matchedPegInAmount)
                matchedChange?.let { hiddenChangeUids += it.uid }

                val amount = matchedPegIn?.let { (index, _) ->
                    usedPegInIndexes += index
                    matchedPegInAmount
                }
                    ?: record.publicMwebNetAmount(matchedChange)

                amount?.let { adjustedAmounts[record.uid] = it.negate() }
            }

        mwebPegIns.forEach { pegIn ->
            records
                .filter { it.uid !in hiddenChangeUids }
                .filter { it.matchesPublicMwebChange(pegIn) }
                .forEach { hiddenChangeUids += it.uid }
        }

        return PublicMwebPegInDisplayChanges(hiddenChangeUids, adjustedAmounts)
    }

    private fun BitcoinTransactionRecord.isPublicMwebPegInMarker(): Boolean {
        // MWEB extension outputs have no public address after BitcoinKit conversion.
        return transactionRecordType == TransactionRecordType.BITCOIN_OUTGOING &&
            hasNoVisibleTo
    }

    private fun BitcoinTransactionRecord.matchPublicMwebChange(
        records: List<BitcoinTransactionRecord>,
        hiddenChangeUids: Set<String>,
        expectedNetAmount: BigDecimal?
    ): BitcoinTransactionRecord? {
        val publicAmount = mainValue.decimalValue?.abs() ?: return null

        return records
            .filter { it.uid !in hiddenChangeUids }
            .filter { abs(timestamp - it.timestamp) <= LITECOIN_MWEB_PEG_IN_MATCH_TIMESTAMP_TOLERANCE_SECONDS }
            .mapNotNull { change ->
                change.publicMwebChangeAmount()
                    ?.takeIf { it < publicAmount }
                    ?.let { changeAmount -> change to changeAmount }
            }
            .minWithOrNull(
                compareBy<Pair<BitcoinTransactionRecord, BigDecimal>> { abs(timestamp - it.first.timestamp) }
                    .thenBy { (_, changeAmount) ->
                        expectedNetAmount
                            ?.let { (publicAmount - changeAmount - it).abs() }
                            ?: BigDecimal.ZERO
                    }
            )
            ?.first
    }

    private fun BitcoinTransactionRecord.isPublicMwebChange(): Boolean {
        val amount = mainValue.decimalValue ?: return false

        return transactionRecordType == TransactionRecordType.BITCOIN_INCOMING &&
            from.isNullOrBlank() &&
            hasNoVisibleTo &&
            amount > BigDecimal.ZERO
    }

    private fun BitcoinTransactionRecord.publicMwebChangeAmount(): BigDecimal? {
        return mainValue.decimalValue?.takeIf { isPublicMwebChange() }
    }

    private val BitcoinTransactionRecord.hasNoVisibleTo: Boolean
        get() = to.orEmpty().all { it.isBlank() }

    private fun BitcoinTransactionRecord.matchesPublicMwebChange(pegIn: MwebTransaction): Boolean {
        return isPublicMwebChange() &&
            abs(timestamp - pegIn.timestamp) <= LITECOIN_MWEB_PEG_IN_MATCH_TIMESTAMP_TOLERANCE_SECONDS
    }

    private fun BitcoinTransactionRecord.publicMwebNetAmount(
        change: BitcoinTransactionRecord?
    ): BigDecimal? {
        val publicAmount = mainValue.decimalValue?.abs() ?: return null
        val changeAmount = change?.mainValue?.decimalValue ?: return null
        val netAmount = publicAmount - changeAmount

        return netAmount.takeIf { it > BigDecimal.ZERO }
    }

    private fun BitcoinTransactionRecord.matchesPublicMwebPegIn(pegIn: MwebTransaction): Boolean {
        if (!isPublicMwebPegInMarker()) return false
        if (abs(timestamp - pegIn.timestamp) > LITECOIN_MWEB_PEG_IN_MATCH_TIMESTAMP_TOLERANCE_SECONDS) {
            return false
        }

        val publicAmount = mainValue.decimalValue?.abs() ?: return false
        val mwebAmount = satoshiToBTC(pegIn.amount).abs()
        return publicAmount >= mwebAmount
    }

    private data class PublicMwebPegInDisplayChanges(
        val hiddenChangeUids: Set<String>,
        val adjustedAmounts: Map<String, BigDecimal>
    ) {
        val isEmpty: Boolean
            get() = hiddenChangeUids.isEmpty() && adjustedAmounts.isEmpty()
    }

    private fun mwebTransactionRecordsFlow(transactionType: FilterTransactionType): Flow<List<TransactionRecord>> {
        if (transactionType == FilterTransactionType.Swap || transactionType == FilterTransactionType.Approve) {
            return emptyFlow()
        }

        return transactionRecordsSubject
            .toFlowable(BackpressureStrategy.BUFFER)
            .map { records -> filterMwebTransactionRecords(records, transactionType) }
            .asFlow()
    }

    private fun filterMwebTransactionRecords(
        records: List<TransactionRecord>,
        transactionType: FilterTransactionType
    ): List<TransactionRecord> {
        return when (transactionType) {
            FilterTransactionType.All -> {
                records
            }
            FilterTransactionType.Incoming,
            FilterTransactionType.Outgoing -> {
                records.filter { it.matchesMwebTransactionType(transactionType) }
            }
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> {
                emptyList()
            }
        }
    }

    @Synchronized
    private fun mwebTransactionRecords(
        transactionType: FilterTransactionType = FilterTransactionType.All
    ): List<TransactionRecord> {
        val records = baseMwebTransactionRecords(mwebStateOrNull()?.transactions.orEmpty())
        return filterMwebTransactionRecords(records, transactionType)
    }

    @Synchronized
    private fun baseMwebTransactionRecords(transactions: List<MwebTransaction>): List<TransactionRecord> {
        if (cachedMwebTransactions === transactions) {
            return cachedMwebTransactionRecords
        }

        val records = transactions
            .map { it.toTransactionRecord() }
            .sortedWith(compareByDescending<TransactionRecord> { it.timestamp }.thenBy { it.uid })

        cachedMwebTransactions = transactions
        cachedMwebTransactionRecords = records
        return records
    }

    @Synchronized
    private fun clearMwebTransactionRecordsCache() {
        cachedMwebTransactions = null
        cachedMwebTransactionRecords = emptyList()
    }

    private fun TransactionRecord.matchesMwebTransactionType(transactionType: FilterTransactionType): Boolean {
        return when (transactionType) {
            FilterTransactionType.All -> true
            FilterTransactionType.Incoming -> transactionRecordType == TransactionRecordType.BITCOIN_INCOMING
            FilterTransactionType.Outgoing -> transactionRecordType == TransactionRecordType.BITCOIN_OUTGOING
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> false
        }
    }

    private fun MwebTransaction.toTransactionRecord(): BitcoinTransactionRecord {
        val recordTimestamp = timestamp.takeIf { it > 0 } ?: System.currentTimeMillis() / 1000
        val recordAddress = address?.takeIf { it.isNotBlank() }
        val canonicalHash = canonicalTransactionHash?.takeIf { it.isNotBlank() }
        val transactionHash = canonicalHash ?: outputIds.firstOrNull() ?: uid
        val transactionRecordType = when (type) {
            MwebTransactionType.Incoming -> TransactionRecordType.BITCOIN_INCOMING
            MwebTransactionType.Outgoing -> TransactionRecordType.BITCOIN_OUTGOING
        }
        val recordAmount = satoshiToBTC(amount).let { value ->
            if (type == MwebTransactionType.Outgoing) value.negate() else value
        }

        return BitcoinTransactionRecord(
            source = wallet.transactionSource,
            token = wallet.token,
            uid = uid,
            transactionHash = transactionHash,
            transactionIndex = 0,
            blockHeight = height,
            confirmationsThreshold = DISPLAY_CONFIRMATIONS_THRESHOLD,
            timestamp = recordTimestamp,
            fee = fee?.let { TransactionValue.CoinValue(wallet.token, satoshiToBTC(it)) },
            failed = false,
            lockInfo = null,
            conflictingHash = null,
            showRawTransaction = false,
            amount = recordAmount,
            from = null,
            to = recordAddress?.let { listOf(it) },
            memo = null,
            changeAddresses = null,
            transactionRecordType = transactionRecordType,
            canonicalTransactionHash = canonicalHash
        )
    }

    override val blockchainType = BlockchainType.Litecoin

    override fun usedAddresses(change: Boolean): List<UsedAddress> =
        if (mwebMode) {
            emptyList()
        } else {
            kit.usedAddresses(change).map { UsedAddress(it.index, it.address, "https://blockchair.com/litecoin/address/${it.address}") }
        }

    override fun availableBalance(
        feeRate: Int,
        address: String?,
        memo: String?,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        changeToFirstInput: Boolean,
        utxoFilters: UtxoFilters
    ): BigDecimal {
        val mwebSource = mwebSendSource(address)
        if (mwebSource == null && !mwebMode) {
            return super.availableBalance(
                feeRate,
                address,
                memo,
                unspentOutputs,
                pluginData,
                changeToFirstInput,
                utxoFilters
            )
        }

        val source = mwebSource ?: LitecoinSendSource.Mweb
        val balance = mwebSpendableBalance(source, unspentOutputs)
        // In MWEB mode the user may press MAX before entering a destination. The wallet's own
        // MWEB receive address is used as a sample so MAX reflects the actual MWEB-spendable
        // balance instead of the small canonical leftover that super.availableBalance returns.
        val destination = address ?: cachedOwnMwebAddress ?: run {
            scheduleOwnMwebAddressFetch()
            return BigDecimal.ZERO
        }
        val request = MwebQuoteRequest(
            source = source,
            feeRate = feeRate,
            address = destination,
            memo = memo,
            pluginData = pluginData.orEmpty().toMap(),
            unspentOutputs = unspentOutputs?.toList(),
            changeToFirstInput = changeToFirstInput,
            filters = utxoFilters
        )
        val cacheKey = MwebAvailableBalanceKey(
            balance = balance,
            request = request
        )

        cachedMwebAvailableBalance(cacheKey)?.let { return it }
        scheduleMwebAvailableBalanceCalculation(
            cacheKey = cacheKey,
            balance = balance,
            request = request
        )
        return BigDecimal.ZERO
    }

    override fun minimumSendAmount(address: String?): BigDecimal? {
        if (mwebSendSource(address) != null) return satoshiToBTC(1)
        return super.minimumSendAmount(address)
    }

    override fun bitcoinFeeInfo(
        amount: BigDecimal,
        feeRate: Int,
        address: String?,
        memo: String?,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        changeToFirstInput: Boolean,
        filters: UtxoFilters
    ): BitcoinFeeInfo? {
        val mwebSource = mwebSendSource(address)
        if (mwebSource == null) {
            return super.bitcoinFeeInfo(
                amount,
                feeRate,
                address,
                memo,
                unspentOutputs,
                pluginData,
                changeToFirstInput,
                filters
            )
        }

        val destination = address ?: return null
        val request = MwebQuoteRequest(
            source = mwebSource,
            feeRate = feeRate,
            address = destination,
            memo = memo,
            pluginData = pluginData.orEmpty().toMap(),
            unspentOutputs = unspentOutputs?.toList(),
            changeToFirstInput = changeToFirstInput,
            filters = filters
        )
        val cacheKey = MwebFeeInfoKey(
            amount = amount,
            request = request
        )
        cachedMwebFeeInfo(cacheKey)?.let { return it.value }

        scheduleMwebFeeInfoCalculation(
            cacheKey = cacheKey,
            amount = amount,
            request = request
        )
        return null
    }

    private fun mwebFeeInfo(
        amount: BigDecimal,
        request: MwebQuoteRequest
    ): BitcoinFeeInfo? {
        val value = atomicAmount(amount) ?: return null
        return mwebSendInfo(
            value = value,
            request = request
        )?.toBitcoinFeeInfo(request.source)
    }

    private suspend fun maximumSpendableMwebValue(
        balance: Long,
        request: MwebQuoteRequest
    ): Long {
        if (balance <= 0) return 0
        if (mwebSendInfo(balance, request) != null) return balance

        val fee = mwebSendInfo(1, request)?.sendInfo?.totalFee
            ?: return 0
        val candidate = balance - fee
        if (candidate <= 0) return 0
        if (mwebSendInfo(candidate, request) != null) return candidate

        return searchMaximumSpendableMwebValue(
            high = candidate - 1,
            request = request
        )
    }

    private suspend fun searchMaximumSpendableMwebValue(
        high: Long,
        request: MwebQuoteRequest
    ): Long {
        var left = 1L
        var right = high
        var best = 0L

        while (left <= right) {
            yield()
            val amount = left + (right - left) / 2
            if (mwebSendInfo(amount, request) == null) {
                right = amount - 1
            } else {
                best = amount
                left = amount + 1
            }
        }

        return best
    }

    private fun mwebSendInfo(
        value: Long,
        request: MwebQuoteRequest
    ): LitecoinSendInfo.Mweb? {
        return try {
            kit.sendInfo(
                value = value,
                address = request.address,
                memo = request.memo,
                source = request.source,
                feeRate = request.feeRate,
                unspentOutputs = request.unspentOutputs.takeIf { request.source == LitecoinSendSource.Public },
                pluginData = request.pluginData,
                changeToFirstInput = request.changeToFirstInput && request.source == LitecoinSendSource.Public,
                filters = request.filters
            ) as? LitecoinSendInfo.Mweb
        } catch (_: Throwable) {
            null
        }
    }

    private fun mwebSendSource(address: String?): LitecoinSendSource? {
        val destination = address ?: return null
        return when {
            mwebMode -> LitecoinSendSource.Mweb
            kit.isMwebAddress(destination) -> LitecoinSendSource.Public
            else -> null
        }
    }

    private fun mwebSpendableBalance(
        source: LitecoinSendSource,
        unspentOutputs: List<UnspentOutputInfo>?,
    ): Long {
        return when (source) {
            LitecoinSendSource.Mweb -> {
                kit.litecoinBalance.mweb?.confirmed ?: 0
            }
            LitecoinSendSource.Public -> {
                unspentOutputs?.sumOf { it.value } ?: kit.litecoinBalance.publicSpendable
            }
            LitecoinSendSource.Auto -> 0
        }
    }

    private fun LitecoinSendInfo.toBitcoinFeeInfo(source: LitecoinSendSource): BitcoinFeeInfo {
        return when (this) {
            is LitecoinSendInfo.Mweb -> BitcoinFeeInfo(
                unspentOutputs = emptyList(),
                fee = satoshiToBTC(sendInfo.totalFee),
                changeValue = sendInfo.changeValue?.let { satoshiToBTC(it) },
                changeAddress = null,
                selectedUtxoCount = when (source) {
                    LitecoinSendSource.Public -> sendInfo.selectedPublicUtxos.size
                    LitecoinSendSource.Mweb,
                    LitecoinSendSource.Auto -> 0
                }
            )

            is LitecoinSendInfo.Public -> BitcoinFeeInfo(
                unspentOutputs = sendInfo.unspentOutputs,
                fee = satoshiToBTC(sendInfo.fee),
                changeValue = sendInfo.changeValue?.let { satoshiToBTC(it) },
                changeAddress = sendInfo.changeAddress
            )
        }
    }

    private fun atomicAmount(amount: BigDecimal): Long? {
        return tryOrNull { amount.movePointRight(decimal).longValueExact() }
    }

    override fun validate(address: String, pluginData: Map<Byte, IPluginData>?) {
        if (kit.isMwebAddress(address)) return
        super.validate(address, pluginData)
    }

    override fun isMwebAddress(address: String): Boolean {
        return kit.isMwebAddress(address)
    }

    override suspend fun send(
        amount: BigDecimal,
        address: String,
        memo: String?,
        feeRate: Int,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        transactionSorting: TransactionDataSortMode?,
        rbfEnabled: Boolean,
        changeToFirstInput: Boolean,
        utxoFilters: UtxoFilters
    ): String {
        val mwebSource = mwebSendSource(address)
        if (mwebSource == null) {
            return super.send(
                amount,
                address,
                memo,
                feeRate,
                unspentOutputs,
                pluginData,
                transactionSorting,
                rbfEnabled,
                changeToFirstInput,
                utxoFilters
            )
        }

        val value = atomicAmount(amount) ?: throw LocalizedException(R.string.litecoin_mweb_invalid_amount)
        val sentAt = System.currentTimeMillis() / 1000
        val result = kit.send(
            address = address,
            memo = memo,
            value = value,
            source = mwebSource,
            feeRate = feeRate,
            sortType = getTransactionSortingType(transactionSorting),
            unspentOutputs = unspentOutputs.takeIf { mwebSource == LitecoinSendSource.Public },
            pluginData = pluginData.orEmpty(),
            rbfEnabled = rbfEnabled && mwebSource == LitecoinSendSource.Public,
            changeToFirstInput = changeToFirstInput && mwebSource == LitecoinSendSource.Public,
            filters = utxoFilters
        )

        return when (result) {
            is LitecoinSendResult.Public -> result.transaction.header.uid
            is LitecoinSendResult.Mweb -> {
                mwebSendTransactionId(result.transaction, address, value, sentAt)
            }
        }
    }

    private fun mwebSendTransactionId(
        transaction: MwebSendResult,
        address: String,
        value: Long,
        sentAt: Long
    ): String {
        transaction.canonicalTransactionHash?.takeIf { it.isNotBlank() }?.let { return it }
        transaction.outputIds.firstOrNull { it.isNotBlank() }?.let { return it }

        return matchingLocalMwebTransactionUid(address, value, sentAt)
            ?: transaction.rawTransaction.mwebLocalFallbackId()
    }

    private fun matchingLocalMwebTransactionUid(address: String, value: Long, sentAt: Long): String? {
        return mwebStateOrNull()?.transactions
            ?.asSequence()
            ?.filter { it.type == MwebTransactionType.Outgoing }
            ?.filter { it.amount == value }
            ?.filter { it.address?.equals(address, ignoreCase = true) == true }
            ?.minWithOrNull(
                compareBy<MwebTransaction> { if (it.pending) 0 else 1 }
                    .thenBy { abs(it.timestamp - sentAt) }
                    .thenByDescending { it.timestamp }
            )
            ?.uid
    }

    private fun mwebStateOrNull(): LitecoinMwebState? {
        if (stopped) return null

        return tryOrNull { kit.mwebState }
    }

    private fun ByteArray.mwebLocalFallbackId(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this).toRawHexString()
        return "mweb-local:$digest"
    }

    @Synchronized
    private fun cachedMwebAvailableBalance(key: MwebAvailableBalanceKey): BigDecimal? {
        return mwebAvailableBalanceCache[key]
    }

    private fun <K, V> MutableMap<K, V>.putLimited(key: K, value: V) {
        if (!containsKey(key) && size >= MWEB_QUOTE_CACHE_LIMIT) {
            remove(keys.first())
        }
        put(key, value)
    }

    @Synchronized
    private fun scheduleOwnMwebAddressFetch() {
        if (cachedOwnMwebAddress != null) return
        if (ownMwebAddressFetchJob?.isActive == true) return

        ownMwebAddressFetchJob = scope.launch(dispatcherProvider.default) {
            val fetched = try {
                kit.receiveAddress(LitecoinReceiveAddressType.Mweb)
            } catch (_: Throwable) {
                null
            }
            if (fetched != null) {
                synchronized(this@LitecoinAdapter) {
                    cachedOwnMwebAddress = fetched
                }
                mwebQuoteUpdatedSubject.onNext(Unit)
            }
        }
    }

    @Synchronized
    private fun clearMwebQuoteCache() {
        mwebAvailableBalanceCache.clear()
        mwebFeeInfoCache.clear()
        mwebAvailableBalanceCalculationJob?.cancel()
        mwebAvailableBalanceCalculationJob = null
        mwebAvailableBalanceCalculationKey = null
        mwebFeeInfoCalculationJob?.cancel()
        mwebFeeInfoCalculationJob = null
        mwebFeeInfoCalculationKey = null
    }

    @Synchronized
    private fun cachedMwebFeeInfo(key: MwebFeeInfoKey): CachedMwebFeeInfo? {
        return mwebFeeInfoCache[key]
    }

    private fun scheduleMwebAvailableBalanceCalculation(
        cacheKey: MwebAvailableBalanceKey,
        balance: Long,
        request: MwebQuoteRequest
    ) {
        if (!startMwebAvailableBalanceCalculation(cacheKey)) return

        val job = scope.launch(dispatcherProvider.default) {
            val availableBalance = try {
                satoshiToBTC(
                    maximumSpendableMwebValue(
                        balance = balance,
                        request = request
                    )
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                BigDecimal.ZERO
            }

            val accepted = finishMwebAvailableBalanceCalculation(cacheKey, availableBalance)
            // Zero results aren't cached and don't change the displayed value, so emitting
            // here would just spin balanceUpdatedFlow -> refresh -> dry-run forever.
            if (accepted && availableBalance.signum() != 0) {
                mwebQuoteUpdatedSubject.onNext(Unit)
            }
        }

        setMwebAvailableBalanceCalculationJob(cacheKey, job)
    }

    private fun scheduleMwebFeeInfoCalculation(
        cacheKey: MwebFeeInfoKey,
        amount: BigDecimal,
        request: MwebQuoteRequest
    ) {
        if (!startMwebFeeInfoCalculation(cacheKey)) return

        val job = scope.launch(dispatcherProvider.default) {
            val feeInfo = try {
                mwebFeeInfo(
                    amount = amount,
                    request = request
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                null
            }

            val accepted = finishMwebFeeInfoCalculation(cacheKey, feeInfo)
            if (accepted) {
                mwebQuoteUpdatedSubject.onNext(Unit)
            }
        }

        setMwebFeeInfoCalculationJob(cacheKey, job)
    }

    @Synchronized
    private fun startMwebAvailableBalanceCalculation(key: MwebAvailableBalanceKey): Boolean {
        if (mwebAvailableBalanceCache.containsKey(key)) return false
        if (mwebAvailableBalanceCalculationKey == key) return false

        mwebAvailableBalanceCalculationJob?.cancel()
        mwebAvailableBalanceCalculationJob = null
        mwebAvailableBalanceCalculationKey = key
        return true
    }

    @Synchronized
    private fun setMwebAvailableBalanceCalculationJob(key: MwebAvailableBalanceKey, job: Job) {
        if (mwebAvailableBalanceCalculationKey == key) {
            mwebAvailableBalanceCalculationJob = job
        } else {
            job.cancel()
        }
    }

    @Synchronized
    private fun finishMwebAvailableBalanceCalculation(
        key: MwebAvailableBalanceKey,
        value: BigDecimal
    ): Boolean {
        if (mwebAvailableBalanceCalculationKey != key) return false

        // Skip caching zero results: they typically reflect a transient gate (maturity,
        // fee margin) that may resolve on the next tip without any balance change.
        if (value.signum() != 0) {
            mwebAvailableBalanceCache.putLimited(key, value)
        }
        mwebAvailableBalanceCalculationKey = null
        mwebAvailableBalanceCalculationJob = null
        return true
    }

    @Synchronized
    private fun startMwebFeeInfoCalculation(key: MwebFeeInfoKey): Boolean {
        if (mwebFeeInfoCache.containsKey(key)) return false
        if (mwebFeeInfoCalculationKey == key) return false

        mwebFeeInfoCalculationJob?.cancel()
        mwebFeeInfoCalculationJob = null
        mwebFeeInfoCalculationKey = key
        return true
    }

    @Synchronized
    private fun setMwebFeeInfoCalculationJob(key: MwebFeeInfoKey, job: Job) {
        if (mwebFeeInfoCalculationKey == key) {
            mwebFeeInfoCalculationJob = job
        } else {
            job.cancel()
        }
    }

    @Synchronized
    private fun finishMwebFeeInfoCalculation(
        key: MwebFeeInfoKey,
        value: BitcoinFeeInfo?
    ): Boolean {
        if (mwebFeeInfoCalculationKey != key) return false

        mwebFeeInfoCache.putLimited(key, CachedMwebFeeInfo(value))
        mwebFeeInfoCalculationKey = null
        mwebFeeInfoCalculationJob = null
        return true
    }

    override fun isTransactionInSendQueue(txHash: String): Boolean {
        return !mwebMode && super.isTransactionInSendQueue(txHash)
    }

    @Synchronized
    private fun mwebAdapterState(): AdapterState {
        cachedMwebAdapterState?.let { return it }

        val mwebState = currentMwebSyncState ?: return AdapterState.Connecting
        if (mwebState.blockHeaderHeight <= 0) return AdapterState.Connecting

        val publicState = super.balanceState
        val publicLastBlockInfo = super.lastBlockInfo
        val publicSyncingState = publicState as? AdapterState.Syncing
        val publicHeaderSyncState = publicSyncingState?.takeIf { it.isPublicHeaderSync(mwebState) }
        val publicTipHeight = mwebPublicTipHeight(publicLastBlockInfo, publicSyncingState)

        val state = when {
            publicHeaderSyncState != null -> publicHeaderSyncState
            publicTipHeight == null -> AdapterState.Connecting
            mwebState.isSynced(publicTipHeight = publicTipHeight) -> AdapterState.Synced
            else -> AdapterState.Syncing(progress = mwebSyncProgress(mwebState, publicTipHeight))
        }
        cachedMwebAdapterState = state
        return state
    }

    @Synchronized
    private fun clearMwebAdapterStateCache() {
        cachedMwebAdapterState = null
    }

    private fun mwebSyncProgress(state: MwebSyncState, tipHeight: Int): Double? {
        if (tipHeight <= MwebNetworkPolicy.MAINNET_ACTIVATION_HEIGHT) return null

        val phases = listOf(
            state.blockHeaderHeight,
            state.mwebHeaderHeight,
            state.mwebUtxosHeight
        )
        return phases
            .map { it.mwebSyncPhaseProgress(tipHeight) }
            .average()
            .let { it * 100.0 }
            .coerceIn(0.0, 100.0)
    }

    private fun mwebPublicTipHeight(
        publicLastBlockInfo: LastBlockInfo?,
        publicSyncingState: AdapterState.Syncing?
    ): Int? {
        val publicHeight = publicLastBlockInfo?.height?.takeIf { it > 0 } ?: return null
        val blocksRemained = publicSyncingState?.blocksRemained
        return blocksRemained
            ?.let { publicHeight.toLong() + it }
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: publicHeight
    }

    private fun AdapterState.Syncing.isPublicHeaderSync(state: MwebSyncState): Boolean {
        if (state.mwebHeaderHeight > 0 || state.mwebUtxosHeight > 0) return false

        return substatus != null ||
            blocksRemained?.let { it > 0 } == true ||
            progress?.let { it in 0.0..99.9999 } == true
    }

    private fun Int.mwebSyncPhaseProgress(tipHeight: Int): Double {
        val startHeight = MwebNetworkPolicy.MAINNET_ACTIVATION_HEIGHT
        val height = takeIf { it > 0 } ?: startHeight
        return ((height - startHeight).toDouble() / (tipHeight - startHeight).toDouble())
            .coerceIn(0.0, 1.0)
    }

    private data class MwebQuoteRequest(
        val source: LitecoinSendSource,
        val feeRate: Int,
        val address: String,
        val memo: String?,
        val pluginData: Map<Byte, IPluginData>,
        val unspentOutputs: List<UnspentOutputInfo>?,
        val changeToFirstInput: Boolean,
        val filters: UtxoFilters
    )

    private data class MwebAvailableBalanceKey(
        val balance: Long,
        val request: MwebQuoteRequest
    )

    private data class MwebFeeInfoKey(
        val amount: BigDecimal,
        val request: MwebQuoteRequest
    )

    private data class CachedMwebFeeInfo(
        val value: BitcoinFeeInfo?
    )

    companion object {
        private const val KIT_CONFIRMATIONS_THRESHOLD = 1
        private const val DISPLAY_CONFIRMATIONS_THRESHOLD = 3
        private const val MWEB_QUOTE_CACHE_LIMIT = 20
        private const val BLOCKCHAIR_EXPLORER_TITLE = "blockchair.com"
        private const val MWEB_EXPLORER_TITLE = "mwebexplorer.com"
        private const val MWEB_EXPLORER_BLOCK_URL = "https://www.mwebexplorer.com/blocks/block/"

        private fun blockchairTransactionUrl(transactionHash: String) =
            "https://blockchair.com/litecoin/transaction/$transactionHash"

        private data class LitecoinKitCreateContext(
            val wallet: Wallet,
            val syncMode: BitcoinCore.SyncMode,
            val purpose: HDWallet.Purpose,
            val sharedPeerGroupHolder: SharedPeerGroupHolder,
            val mwebConfig: MwebConfig?,
            val dispatcherProvider: DispatcherProvider
        ) {
            val account: Account
                get() = wallet.account
        }

        private fun createKit(
            wallet: Wallet,
            syncMode: BitcoinCore.SyncMode,
            mode: Mode,
            restoreSettings: RestoreSettings? = null,
            dispatcherProvider: DispatcherProvider,
        ): LitecoinKit {
            val account = wallet.account
            if (mode == Mode.Mweb && account.type !is AccountType.Mnemonic) {
                throw UnsupportedAccountException()
            }
            val purpose = when (mode) {
                is Mode.Public -> mode.derivation.purpose
                Mode.Mweb -> TokenType.Derivation.Bip84.purpose
            }
            val sharedPeerGroupHolder = LitecoinKit.getOrCreateSharedPeerGroup(
                context = App.instance,
                walletId = account.id,
                networkType = NetworkType.MainNet
            )
            val mwebConfig = when (mode) {
                is Mode.Public -> restoreSettings?.let { mwebConfig(it, dispatcherProvider) }
                Mode.Mweb -> mwebConfig(restoreSettings, dispatcherProvider)
            }

            val context = LitecoinKitCreateContext(
                wallet = wallet,
                syncMode = syncMode,
                purpose = purpose,
                sharedPeerGroupHolder = sharedPeerGroupHolder,
                mwebConfig = mwebConfig,
                dispatcherProvider = dispatcherProvider
            )
            return when (val accountType = account.type) {
                is AccountType.HdExtendedKey -> createExtendedKeyKit(accountType, context)
                is AccountType.Mnemonic -> createMnemonicKit(accountType, context)
                is AccountType.BitcoinAddress -> createWatchAddressKit(accountType, context)
                is AccountType.HardwareCard -> createHardwareKit(context)
                is AccountType.TrezorDevice -> createTrezorKit(context)
                else -> throw UnsupportedAccountException()
            }
        }

        private fun createExtendedKeyKit(
            accountType: AccountType.HdExtendedKey,
            context: LitecoinKitCreateContext
        ): LitecoinKit {
            return LitecoinKit(
                context = App.instance,
                extendedKey = accountType.hdExtendedKey,
                purpose = context.purpose,
                walletId = context.account.id,
                syncMode = context.syncMode,
                networkType = NetworkType.MainNet,
                confirmationsThreshold = KIT_CONFIRMATIONS_THRESHOLD,
                sharedPeerGroupHolder = context.sharedPeerGroupHolder,
                mwebPublicSendConfig = mwebPublicSendConfig(context.dispatcherProvider),
            )
        }

        private fun createMnemonicKit(
            accountType: AccountType.Mnemonic,
            context: LitecoinKitCreateContext
        ): LitecoinKit {
            return LitecoinKit(
                context = App.instance,
                words = accountType.words,
                passphrase = accountType.passphrase,
                walletId = context.account.id,
                syncMode = context.syncMode,
                networkType = NetworkType.MainNet,
                confirmationsThreshold = KIT_CONFIRMATIONS_THRESHOLD,
                purpose = context.purpose,
                sharedPeerGroupHolder = context.sharedPeerGroupHolder,
                mwebConfig = context.mwebConfig,
                mwebPublicSendConfig = mwebPublicSendConfig(context.dispatcherProvider)
            )
        }

        private fun createWatchAddressKit(
            accountType: AccountType.BitcoinAddress,
            context: LitecoinKitCreateContext
        ): LitecoinKit {
            return LitecoinKit(
                context = App.instance,
                watchAddress = accountType.address,
                walletId = context.account.id,
                syncMode = context.syncMode,
                networkType = NetworkType.MainNet,
                confirmationsThreshold = KIT_CONFIRMATIONS_THRESHOLD,
                sharedPeerGroupHolder = context.sharedPeerGroupHolder,
                mwebPublicSendConfig = mwebPublicSendConfig(context.dispatcherProvider)
            )
        }

        private fun createHardwareKit(context: LitecoinKitCreateContext): LitecoinKit {
            val token = context.wallet.token
            val hardwareWalletEcdaBitcoinSigner = buildHardwareWalletEcdaBitcoinSigner(
                accountId = context.account.id,
                blockchainType = token.blockchainType,
                tokenType = token.type,
            )
            val hardwareWalletSchnorrSigner = buildHardwareWalletSchnorrBitcoinSigner(
                accountId = context.account.id,
                blockchainType = token.blockchainType,
                tokenType = token.type,
            )
            return LitecoinKit(
                context = App.instance,
                extendedKey = context.wallet.getHDExtendedKey()
                    ?: throw IllegalStateException(
                        Translator.getString(R.string.litecoin_hardware_extended_public_key_required)
                    ),
                purpose = context.purpose,
                walletId = context.account.id,
                syncMode = context.syncMode,
                networkType = NetworkType.MainNet,
                confirmationsThreshold = KIT_CONFIRMATIONS_THRESHOLD,
                iInputSigner = hardwareWalletEcdaBitcoinSigner,
                iSchnorrInputSigner = hardwareWalletSchnorrSigner,
                sharedPeerGroupHolder = context.sharedPeerGroupHolder,
                mwebPublicSendConfig = mwebPublicSendConfig(context.dispatcherProvider)
            )
        }

        private fun createTrezorKit(context: LitecoinKitCreateContext): LitecoinKit {
            val token = context.wallet.token
            val trezorSigner = buildTrezorBtcSigner(
                accountId = context.account.id,
                blockchainType = token.blockchainType,
                coin = "Litecoin"
            )
            return LitecoinKit(
                context = App.instance,
                extendedKey = requireNotNull(context.wallet.getHDExtendedKey()),
                purpose = context.purpose,
                walletId = context.account.id,
                syncMode = context.syncMode,
                networkType = NetworkType.MainNet,
                confirmationsThreshold = KIT_CONFIRMATIONS_THRESHOLD,
                iInputSigner = trezorSigner,
                iSchnorrInputSigner = trezorSigner,
                sharedPeerGroupHolder = context.sharedPeerGroupHolder,
                mwebPublicSendConfig = mwebPublicSendConfig(context.dispatcherProvider)
            )
        }

        private fun mwebPublicSendConfig(dispatcherProvider: DispatcherProvider): MwebPublicSendConfig {
            return MwebPublicSendConfig(dispatcherProvider = mwebDispatcherProvider(dispatcherProvider))
        }

        private fun mwebConfig(
            restoreSettings: RestoreSettings?,
            dispatcherProvider: DispatcherProvider,
        ): MwebConfig {
            return MwebConfig(
                dispatcherProvider = mwebDispatcherProvider(dispatcherProvider),
                restorePoint = restoreSettings.mwebRestorePoint()
            )
        }

        private fun mwebDispatcherProvider(dispatcherProvider: DispatcherProvider): CoroutineMwebDispatcherProvider {
            return CoroutineMwebDispatcherProvider(
                io = dispatcherProvider.io,
                callback = dispatcherProvider.default
            )
        }

        private fun RestoreSettings?.mwebRestorePoint(): MwebRestorePoint {
            val height = LitecoinMwebRestoreHeight.toBlockHeight(this?.birthdayHeight)
            return height?.let { MwebRestorePoint.BlockHeight(it) } ?: MwebRestorePoint.Activation
        }

        fun clear(walletId: String) {
            LitecoinKit.clear(App.instance, NetworkType.MainNet, walletId)
        }

        /**
         * Clears only MWEB scan storage through LitecoinKit without touching public Litecoin DBs.
         */
        fun clearMweb(walletId: String) {
            LitecoinKit.clearMweb(App.instance, NetworkType.MainNet, walletId)
        }

        fun firstAddress(accountType: AccountType, tokenType: TokenType) : String {
            if (tokenType == TokenType.Mweb) throw UnsupportedAccountException()

            when (accountType) {
                is AccountType.Mnemonic -> {
                    val seed = accountType.seed
                    val derivation = tokenType.derivation ?: throw IllegalArgumentException()

                    val address = LitecoinKit.firstAddress(
                        seed,
                        derivation.purpose,
                        NetworkType.MainNet
                    )

                    return address.stringValue
                }
                is AccountType.HdExtendedKey -> {
                    val key = accountType.hdExtendedKey
                    val derivation = tokenType.derivation ?: throw IllegalArgumentException()
                    val address = LitecoinKit.firstAddress(
                        key,
                        derivation.purpose,
                        NetworkType.MainNet
                    )

                    return address.stringValue
                }
                is AccountType.BitcoinAddress -> {
                    return accountType.address
                }
                is AccountType.EvmAddress,
                is AccountType.EvmPrivateKey,
                is AccountType.HardwareCard,
                is AccountType.TrezorDevice,
                is AccountType.MnemonicMonero,
                is AccountType.SolanaAddress,
                is AccountType.TonAddress,
                is AccountType.TronAddress,
                is AccountType.StellarAddress,
                is AccountType.StellarSecretKey,
                is AccountType.ZCashUfvKey -> throw UnsupportedAccountException()
            }
        }

    }
}
