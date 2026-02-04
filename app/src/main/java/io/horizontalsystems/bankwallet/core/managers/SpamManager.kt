package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.adapters.EvmTransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.StellarTransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.TronTransactionsAdapter
import io.horizontalsystems.bankwallet.core.storage.ScannedTransactionStorage
import io.horizontalsystems.bankwallet.entities.ScannedTransaction
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.core.hexStringToByteArrayOrNull
import io.horizontalsystems.ethereumkit.decorations.IncomingDecoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.room.StellarAsset
import io.horizontalsystems.tronkit.decoration.NativeTransactionDecoration
import io.horizontalsystems.tronkit.decoration.trc20.OutgoingTrc20Decoration
import io.horizontalsystems.tronkit.decoration.trc20.Trc20TransferEvent
import io.horizontalsystems.tronkit.models.TransferContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import io.horizontalsystems.stellarkit.room.Operation as StellarOperation
import io.horizontalsystems.tronkit.decoration.UnknownTransactionDecoration as TronUnknownTransactionDecoration
import io.horizontalsystems.tronkit.models.FullTransaction as TronFullTransaction

private fun isEvmBlockchain(source: TransactionSource): Boolean {
    return EvmBlockchainManager.blockchainTypes.contains(source.blockchain.type)
}

private fun isTronBlockchain(source: TransactionSource): Boolean {
    return source.blockchain.type == BlockchainType.Tron
}

private fun isStellarBlockchain(source: TransactionSource): Boolean {
    return source.blockchain.type == BlockchainType.Stellar
}

class SpamManager(
    private val localStorage: ILocalStorage,
    private val scannedTransactionStorage: ScannedTransactionStorage,
    private val contactsRepository: ContactsRepository,
    private val transactionAdapterManager: TransactionAdapterManager
) {
    private val poisoningScorer = PoisoningScorer()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Lazy initialized SpamRescanManager to avoid circular dependency
    private val spamRescanManager: SpamRescanManager by lazy {
        SpamRescanManager(scannedTransactionStorage, this)
    }

    // Cache for trusted addresses from contacts (blockchainType:address -> true)
    // Key format: "blockchainTypeUid:lowercaseAddress" for fast lookup
    @Volatile
    private var trustedAddressesCache: Set<String> = emptySet()

    companion object {
        private const val OUTGOING_CONTEXT_SIZE = 10
    }

    init {
        // Subscribe to contacts updates to keep cache in sync
        coroutineScope.launch {
            contactsRepository.contactsFlow.collect { contacts ->
                updateTrustedAddressesCache(contacts)
            }
        }
    }

    private fun updateTrustedAddressesCache(contacts: List<Contact>) {
        trustedAddressesCache = contacts
            .flatMap { contact ->
                contact.addresses.map { addr ->
                    "${addr.blockchain.type.uid}:${addr.address.lowercase()}"
                }
            }
            .toSet()
    }

    private fun isAddressTrusted(address: String, blockchainType: BlockchainType): Boolean {
        val key = "${blockchainType.uid}:${address.lowercase()}"
        return trustedAddressesCache.contains(key)
    }

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
        private set

    fun updateFilterHideSuspiciousTx(hide: Boolean) {
        localStorage.hideSuspiciousTransactions = hide
        hideSuspiciousTx = hide
    }

    fun findSpamByAddress(address: String): ScannedTransaction? {
        return scannedTransactionStorage.findSpamByAddress(address)
    }

    /**
     * Fetch recent outgoing transactions from adapter to build context for spam detection.
     * Used as fallback when transaction is not in database and full scan is not available.
     */
    private fun getOutgoingContextFromAdapter(
        source: TransactionSource,
        limit: Int = OUTGOING_CONTEXT_SIZE
    ): List<PoisoningScorer.OutgoingTxInfo> {
        val adapter = transactionAdapterManager.adaptersMap[source] ?: return emptyList()
        val evmAdapter = adapter as? EvmTransactionsAdapter ?: return emptyList()
        val userAddress = evmAdapter.evmKitWrapper.evmKit.receiveAddress

        return try {
            runBlocking {
                val transactions = adapter.getFullTransactionsBefore(null, limit)
                transactions
                    .sortedByDescending { it.transaction.timestamp }
                    .mapNotNull { extractOutgoingInfo(it, userAddress) }
                    .take(limit)
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Check if transaction is spam.
     * First checks stored result in database. If not found, triggers background scan
     * and waits for result to ensure accurate spam detection with full outgoing context.
     * Addresses in user's contacts are trusted and never flagged as spam.
     */
    fun isSpam(
        transactionHash: ByteArray,
        events: List<TransferEvent>,
        timestamp: Long,
        blockHeight: Int?,
        source: TransactionSource
    ): Boolean {
        // Check database first for stored result
        scannedTransactionStorage.getScannedTransaction(transactionHash)?.let {
            return it.isSpam
        }

        val blockchainType = source.blockchain.type

        // Check if any event address is from a trusted contact - if so, not spam
        val eventAddresses = events.mapNotNull { it.address }
        if (eventAddresses.any { isAddressTrusted(it, blockchainType) }) {
            // Trusted address - save as score 0 and return not spam
            saveSpamResult(transactionHash, 0, blockchainType, null)
            return false
        }

        // Not in database - trigger on-demand scan and wait for result
        // This ensures proper outgoing context is built for accurate spam detection
        if (isEvmBlockchain(source) || isTronBlockchain(source) || isStellarBlockchain(source)) {
            val adapter = transactionAdapterManager.adaptersMap[source]
            if (adapter != null) {
                runBlocking {
                    spamRescanManager.ensureTransactionScanned(transactionHash, source, adapter)
                }

                // Check database again after scan
                scannedTransactionStorage.getScannedTransaction(transactionHash)?.let {
                    return it.isSpam
                }
            }
        }

        // Fallback: calculate with context fetched from adapter
        val recentOutgoingTxs = getOutgoingContextFromAdapter(source)

        val scoringResult = poisoningScorer.calculateSpamScore(
            events = events,
            incomingTimestamp = timestamp,
            incomingBlockHeight = blockHeight,
            recentOutgoingTxs = recentOutgoingTxs
        )

        // Save result to database
        saveSpamResult(transactionHash, scoringResult.score, blockchainType, scoringResult.spamAddress)

        return scoringResult.score >= PoisoningScorer.SPAM_THRESHOLD
    }

    /**
     * Get base token for a transaction source.
     */
    fun getBaseToken(source: TransactionSource): Token? {
        return when (val adapter = transactionAdapterManager.adaptersMap[source]) {
            is EvmTransactionsAdapter -> adapter.baseToken
            is TronTransactionsAdapter -> {
                App.coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native))
            }
            is StellarTransactionsAdapter -> {
                App.coinManager.getToken(TokenQuery(BlockchainType.Stellar, TokenType.Native))
            }
            else -> null
        }
    }

    // ==================== Common Processing Logic ====================

    /**
     * Save spam detection result to database.
     */
    private fun saveSpamResult(
        transactionHash: ByteArray,
        spamScore: Int,
        blockchainType: BlockchainType,
        spamAddress: String?
    ) {
        try {
            scannedTransactionStorage.save(
                ScannedTransaction(
                    transactionHash = transactionHash,
                    spamScore = spamScore,
                    blockchainType = blockchainType,
                    address = spamAddress
                )
            )
        } catch (_: Throwable) {
        }
    }

    /**
     * Common method to process extracted events for spam detection.
     * Called by both EVM and Tron processing methods after extracting events.
     */
    private fun processEventsForSpam(
        txHashBytes: ByteArray,
        timestamp: Long,
        blockHeight: Int?,
        incomingEvents: List<TransferEvent>,
        source: TransactionSource,
        outgoingContext: List<PoisoningScorer.OutgoingTxInfo>
    ) {
        // Skip if already scanned
        scannedTransactionStorage.getScannedTransaction(txHashBytes)?.let { return }

        val blockchainType = source.blockchain.type

        // Calculate spam score
        val scoringResult = if (incomingEvents.isEmpty()) {
            // No incoming events = score 0 (outgoing, contract calls, etc.)
            PoisoningScorer.SpamScoringResult(0, null)
        } else {
            // Check if any event address is from a trusted contact
            val eventAddresses = incomingEvents.mapNotNull { it.address }
            if (eventAddresses.any { isAddressTrusted(it, blockchainType) }) {
                PoisoningScorer.SpamScoringResult(0, null)
            } else {
                poisoningScorer.calculateSpamScore(
                    events = incomingEvents,
                    incomingTimestamp = timestamp,
                    incomingBlockHeight = blockHeight,
                    recentOutgoingTxs = outgoingContext
                )
            }
        }

        // Save result to database
        saveSpamResult(txHashBytes, scoringResult.score, blockchainType, scoringResult.spamAddress)
    }

    // ==================== EVM Transaction Processing ====================

    /**
     * Extract outgoing transaction info from EVM FullTransaction.
     */
    fun extractOutgoingInfo(
        fullTx: FullTransaction,
        userAddress: Address
    ): PoisoningScorer.OutgoingTxInfo? {
        val tx = fullTx.transaction
        return when (val decoration = fullTx.decoration) {
            is OutgoingDecoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.eip55, tx.timestamp, tx.blockNumber?.toInt())
            }
            is OutgoingEip20Decoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.eip55, tx.timestamp, tx.blockNumber?.toInt())
            }
            is UnknownTransactionDecoration -> {
                if (tx.from == userAddress) {
                    decoration.eventInstances
                        .mapNotNull { it as? TransferEventInstance }
                        .filter { it.from == userAddress }
                        .firstOrNull()?.let { transfer ->
                            PoisoningScorer.OutgoingTxInfo(transfer.to.eip55, tx.timestamp, tx.blockNumber?.toInt())
                        }
                } else null
            }
            else -> null
        }
    }

    /**
     * Extract incoming events from EVM FullTransaction.
     */
    private fun extractIncomingEvents(
        fullTx: FullTransaction,
        userAddress: Address,
        baseToken: Token
    ): List<TransferEvent> {
        val tx = fullTx.transaction
        return when (val decoration = fullTx.decoration) {
            is IncomingDecoration -> {
                val value = decoration.value.toBigDecimal(baseToken.decimals)
                listOf(TransferEvent(decoration.from.eip55, TransactionValue.CoinValue(baseToken, value)))
            }
            is UnknownTransactionDecoration -> {
                if (tx.from != userAddress) {
                    decoration.eventInstances
                        .mapNotNull { it as? TransferEventInstance }
                        .map { transfer ->
                            val tokenValue = transfer.tokenInfo?.let { info ->
                                TransactionValue.TokenValue(
                                    tokenName = info.tokenName,
                                    tokenCode = info.tokenSymbol,
                                    tokenDecimals = info.tokenDecimal,
                                    value = transfer.value.toBigDecimal(info.tokenDecimal),
                                )
                            } ?: TransactionValue.RawValue(transfer.value)
                            TransferEvent(transfer.to.eip55, tokenValue)
                        }
                } else emptyList()
            }
            else -> emptyList()
        }
    }

    /**
     * Process an EVM FullTransaction for spam detection during background rescan.
     */
    fun processEvmTransactionForSpamWithContext(
        fullTx: FullTransaction,
        source: TransactionSource,
        userAddress: Address,
        baseToken: Token,
        outgoingContext: List<PoisoningScorer.OutgoingTxInfo>
    ) {
        val tx = fullTx.transaction
        val incomingEvents = extractIncomingEvents(fullTx, userAddress, baseToken)
        processEventsForSpam(tx.hash, tx.timestamp, tx.blockNumber?.toInt(), incomingEvents, source, outgoingContext)
    }

    // ==================== Tron Transaction Processing ====================

    /**
     * Extract outgoing transaction info from Tron FullTransaction.
     */
    fun extractOutgoingInfo(
        fullTx: TronFullTransaction,
        userAddress: io.horizontalsystems.tronkit.models.Address
    ): PoisoningScorer.OutgoingTxInfo? {
        val tx = fullTx.transaction
        val timestamp = tx.timestamp / 1000
        val blockHeight = tx.blockNumber?.toInt()

        return when (val decoration = fullTx.decoration) {
            is NativeTransactionDecoration -> {
                val contract = decoration.contract as? TransferContract ?: return null
                if (contract.ownerAddress == userAddress) {
                    PoisoningScorer.OutgoingTxInfo(contract.toAddress.base58, timestamp, blockHeight)
                } else null
            }
            is OutgoingTrc20Decoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.base58, timestamp, blockHeight)
            }
            is TronUnknownTransactionDecoration -> {
                if (decoration.fromAddress == userAddress) {
                    decoration.events
                        .mapNotNull { it as? Trc20TransferEvent }
                        .filter { it.from == userAddress }
                        .firstOrNull()?.let { transfer ->
                            PoisoningScorer.OutgoingTxInfo(transfer.to.base58, timestamp, blockHeight)
                        }
                } else null
            }
            else -> null
        }
    }

    /**
     * Extract incoming events from Tron FullTransaction.
     */
    private fun extractIncomingEvents(
        fullTx: TronFullTransaction,
        userAddress: io.horizontalsystems.tronkit.models.Address,
        baseToken: Token
    ): List<TransferEvent> {
        val tx = fullTx.transaction
        return when (val decoration = fullTx.decoration) {
            is NativeTransactionDecoration -> {
                val contract = decoration.contract as? TransferContract
                if (contract != null && contract.ownerAddress != userAddress && contract.toAddress == userAddress) {
                    val value = contract.amount.toBigDecimal().movePointLeft(baseToken.decimals)
                    listOf(TransferEvent(contract.ownerAddress.base58, TransactionValue.CoinValue(baseToken, value)))
                } else emptyList()
            }
            is TronUnknownTransactionDecoration -> {
                if (decoration.fromAddress != userAddress && decoration.toAddress != userAddress) {
                    decoration.events
                        .mapNotNull { it as? Trc20TransferEvent }
                        .filter { it.to == userAddress && it.from != userAddress }
                        .map { transfer ->
                            val tokenValue = transfer.tokenInfo?.let { info ->
                                TransactionValue.TokenValue(
                                    tokenName = info.tokenName,
                                    tokenCode = info.tokenSymbol,
                                    tokenDecimals = info.tokenDecimal,
                                    value = transfer.value.toBigDecimal().movePointLeft(info.tokenDecimal),
                                )
                            } ?: TransactionValue.RawValue(transfer.value)
                            TransferEvent(transfer.from.base58, tokenValue)
                        }
                } else emptyList()
            }
            else -> emptyList()
        }
    }

    /**
     * Process a Tron FullTransaction for spam detection during background rescan.
     */
    fun processTronTransactionForSpamWithContext(
        fullTx: TronFullTransaction,
        source: TransactionSource,
        userAddress: io.horizontalsystems.tronkit.models.Address,
        baseToken: Token,
        outgoingContext: List<PoisoningScorer.OutgoingTxInfo>
    ) {
        val tx = fullTx.transaction
        val incomingEvents = extractIncomingEvents(fullTx, userAddress, baseToken)
        processEventsForSpam(tx.hash, tx.timestamp / 1000, tx.blockNumber?.toInt(), incomingEvents, source, outgoingContext)
    }

    // ==================== Stellar Transaction Processing ====================

    /**
     * Extract outgoing transaction info from Stellar Operation.
     */
    fun extractOutgoingInfo(
        operation: StellarOperation,
        selfAddress: String
    ): PoisoningScorer.OutgoingTxInfo? {
        // Check payment operations
        operation.payment?.let { payment ->
            if (payment.from == selfAddress) {
                return PoisoningScorer.OutgoingTxInfo(payment.to, operation.timestamp, null)
            }
        }

        // Check account creation operations
        operation.accountCreated?.let { accountCreated ->
            if (accountCreated.funder == selfAddress) {
                return PoisoningScorer.OutgoingTxInfo(accountCreated.account, operation.timestamp, null)
            }
        }

        return null
    }

    /**
     * Extract incoming events from Stellar Operation.
     */
    private fun extractIncomingEvents(
        operation: StellarOperation,
        selfAddress: String,
        baseToken: Token
    ): List<TransferEvent> {
        // Check payment operations
        operation.payment?.let { payment ->
            if (payment.to == selfAddress && payment.from != selfAddress) {
                val token = getStellarToken(payment.asset)
                val transactionValue = if (token != null) {
                    TransactionValue.CoinValue(token, payment.amount)
                } else {
                    TransactionValue.RawValue(payment.amount.unscaledValue())
                }
                return listOf(TransferEvent(payment.from, transactionValue))
            }
        }

        // Check account creation operations
        operation.accountCreated?.let { accountCreated ->
            if (accountCreated.account == selfAddress && accountCreated.funder != selfAddress) {
                val transactionValue = TransactionValue.CoinValue(baseToken, accountCreated.startingBalance)
                return listOf(TransferEvent(accountCreated.funder, transactionValue))
            }
        }

        return emptyList()
    }

    private fun getStellarToken(asset: StellarAsset): Token? {
        val tokenType = when (asset) {
            StellarAsset.Native -> TokenType.Native
            is StellarAsset.Asset -> TokenType.Asset(asset.code, asset.issuer)
        }
        return App.coinManager.getToken(TokenQuery(BlockchainType.Stellar, tokenType))
    }

    /**
     * Process a Stellar Operation for spam detection during background rescan.
     */
    fun processStellarOperationForSpamWithContext(
        operation: StellarOperation,
        source: TransactionSource,
        selfAddress: String,
        baseToken: Token,
        outgoingContext: List<PoisoningScorer.OutgoingTxInfo>
    ) {
        val txHash = operation.transactionHash.hexStringToByteArrayOrNull() ?: return
        val incomingEvents = extractIncomingEvents(operation, selfAddress, baseToken)
        processEventsForSpam(txHash, operation.timestamp, null, incomingEvents, source, outgoingContext)
    }
}