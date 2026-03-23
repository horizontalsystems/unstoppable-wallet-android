package cash.p.terminal.core.usecase

import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.storage.PendingMultiSwapStorage
import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.entities.PendingMultiSwap
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.withContext
import java.math.BigDecimal


class SyncPendingMultiSwapUseCase(
    private val pendingMultiSwapStorage: PendingMultiSwapStorage,
    private val swapProviderTransactionsStorage: SwapProviderTransactionsStorage,
    private val dispatcherProvider: DispatcherProvider,
    private val walletManager: IWalletManager,
    private val transactionAdapterManager: TransactionAdapterManager,
    private val adapterManager: IAdapterManager,
    private val updateSwapProviderTransactionsStatusUseCase: UpdateSwapProviderTransactionsStatusUseCase,
) {
    suspend operator fun invoke() = withContext(dispatcherProvider.io) {
        val swaps = pendingMultiSwapStorage.getAllOnce()
        val hasExecutingOffChain = swaps.any { swap ->
            (swap.leg1Status == PendingMultiSwap.STATUS_EXECUTING && swap.leg1IsOffChain)
            || (swap.leg2Status == PendingMultiSwap.STATUS_EXECUTING && swap.leg2IsOffChain == true)
        }
        if (hasExecutingOffChain) {
            updateProviderStatuses()
        }
        swaps.forEach { swap ->
            syncLeg1(swap)
            syncLeg2(swap)
        }
        deleteCompleted()
    }

    private suspend fun updateProviderStatuses() {
        walletManager.activeWallets.forEach { wallet ->
            val address = adapterManager.getReceiveAddressForWallet(wallet) ?: return@forEach
            tryOrNull { updateSwapProviderTransactionsStatusUseCase(wallet.token, address) }
        }
    }

    // --- off-chain ---

    private suspend fun syncLegOffChain(
        providerTransactionId: String?,
        transactionId: String?,
        providerId: String,
        coinUidOut: String,
        blockchainTypeOut: String,
        amountOut: BigDecimal?,
        legStartTime: Long,
        updateLeg: suspend (String, BigDecimal?) -> Unit,
    ) {
        val swapTx = findSwapProviderTransaction(
            providerTransactionId = providerTransactionId,
            transactionId = transactionId,
            providerId = providerId,
            coinUidOut = coinUidOut,
            blockchainTypeOut = blockchainTypeOut,
            amountOut = amountOut,
            legStartTime = legStartTime,
        ) ?: return

        val newStatus = mapSwapProviderStatus(swapTx.status) ?: return
        updateLeg(newStatus, swapTx.amountOutReal ?: amountOut)
    }

    private suspend fun findSwapProviderTransaction(
        providerTransactionId: String?,
        transactionId: String?,
        providerId: String,
        coinUidOut: String,
        blockchainTypeOut: String,
        amountOut: BigDecimal?,
        legStartTime: Long,
    ): SwapProviderTransaction? {
        providerTransactionId?.let { id ->
            swapProviderTransactionsStorage.getTransaction(id)?.let { return it }
        }

        transactionId?.let { id ->
            swapProviderTransactionsStorage.getByOutgoingRecordUid(id)?.let { return it }
        }

        val swapProvider = PendingMultiSwap.mapProviderIdToSwapProvider(providerId) ?: return null
        val receiveAddress = findReceiveAddress(coinUidOut, blockchainTypeOut) ?: return null
        return swapProviderTransactionsStorage.getByProviderAndTokenOut(
            provider = swapProvider,
            coinUidOut = coinUidOut,
            blockchainTypeOut = blockchainTypeOut,
            addressOut = receiveAddress,
            expectedAmount = amountOut ?: return null,
            legStartTime = legStartTime,
        )
    }

    private fun mapSwapProviderStatus(status: String): String? = when (status) {
        "finished" -> PendingMultiSwap.STATUS_COMPLETED
        "failed", "refunded" -> PendingMultiSwap.STATUS_FAILED
        else -> null
    }

    // --- on-chain ---

    private data class IncomingMatch(
        val amount: BigDecimal?,
        val recordUid: String,
    )

    private suspend fun syncLegOnChain(
        transactionId: String?,
        coinUidIn: String,
        blockchainTypeIn: String,
        coinUidOut: String,
        blockchainTypeOut: String,
        amountOut: BigDecimal?,
        legStartTime: Long,
        updateLeg: suspend (String, BigDecimal?) -> Unit,
        onIncomingMatch: (suspend (IncomingMatch) -> Unit)? = null,
    ) {
        if (checkOutgoingFailed(transactionId, coinUidIn, blockchainTypeIn)) {
            updateLeg(PendingMultiSwap.STATUS_FAILED, null)
            return
        }

        val match = scanIncoming(coinUidOut, blockchainTypeOut, amountOut, legStartTime)
        if (match != null) {
            updateLeg(PendingMultiSwap.STATUS_COMPLETED, match.amount)
            onIncomingMatch?.invoke(match)
        }
    }

    private suspend fun checkOutgoingFailed(
        transactionId: String?,
        coinUidIn: String,
        blockchainTypeIn: String,
    ): Boolean {
        if (transactionId == null) return false
        val wallet = findWallet(coinUidIn, blockchainTypeIn) ?: return false
        val adapter = transactionAdapterManager.getAdapter(wallet.transactionSource) ?: return false

        var from: TransactionRecord? = null
        for (page in 0 until MAX_PAGES) {
            val batch = tryOrNull {
                adapter.getTransactions(from, wallet.token, PAGE_SIZE, FilterTransactionType.Outgoing, null)
            } ?: return false

            val match = batch.firstOrNull { it.transactionHash == transactionId }
            if (match != null) return match.failed

            if (batch.size < PAGE_SIZE) break
            from = batch.last()
        }
        return false
    }

    private suspend fun scanIncoming(
        coinUidOut: String,
        blockchainTypeOut: String,
        expectedAmount: BigDecimal?,
        legStartTime: Long,
    ): IncomingMatch? {
        val wallet = findWallet(coinUidOut, blockchainTypeOut) ?: return null
        val adapter = transactionAdapterManager.getAdapter(wallet.transactionSource) ?: return null
        val startTimeSec = legStartTime / 1000 - START_TIME_BUFFER_SEC
        val lastBlockHeight = adapter.lastBlockInfo?.height

        val candidates = mutableListOf<TransactionRecord>()
        var from: TransactionRecord? = null
        for (page in 0 until MAX_PAGES) {
            val batch = tryOrNull {
                adapter.getTransactions(from, wallet.token, PAGE_SIZE, FilterTransactionType.All, null)
            } ?: break
            if (batch.isEmpty()) break

            for (record in batch) {
                if (record.timestamp >= startTimeSec
                    && !record.failed
                    && record.status(lastBlockHeight) is TransactionStatus.Completed
                ) {
                    candidates.add(record)
                }
            }
            if (batch.size < PAGE_SIZE) break
            from = batch.last()
        }

        if (candidates.isEmpty()) return null
        return matchIncoming(candidates, expectedAmount, legStartTime)
    }

    private fun matchIncoming(
        candidates: List<TransactionRecord>,
        expectedAmount: BigDecimal?,
        legStartTime: Long,
    ): IncomingMatch? {
        if (expectedAmount != null && expectedAmount.signum() > 0) {
            val best = candidates
                .mapNotNull { record ->
                    val amount = extractReceivedAmount(record) ?: return@mapNotNull null
                    val ratio = (amount - expectedAmount).abs().toDouble() / expectedAmount.toDouble()
                    if (ratio <= ON_CHAIN_AMOUNT_TOLERANCE) record to amount else null
                }
                .minByOrNull { (it.second - expectedAmount).abs() }
            return best?.let { IncomingMatch(it.second, it.first.uid) }
        }

        val windowEndSec = (legStartTime + TIGHT_TIME_WINDOW_MS) / 1000
        val startSec = legStartTime / 1000
        val windowCandidates = candidates.filter { it.timestamp in startSec..windowEndSec }
        if (windowCandidates.size == 1) {
            val record = windowCandidates.single()
            return IncomingMatch(extractReceivedAmount(record), record.uid)
        }
        return null
    }

    private fun extractReceivedAmount(record: TransactionRecord): BigDecimal? {
        return when (record) {
            is TonTransactionRecord -> {
                val action = record.actions.singleOrNull()?.type
                when (action) {
                    is TonTransactionRecord.Action.Type.Swap -> action.valueOut.decimalValue?.abs()
                    is TonTransactionRecord.Action.Type.Receive -> action.value.decimalValue?.abs()
                    else -> record.mainValue?.decimalValue?.abs()
                }
            }
            is EvmTransactionRecord -> {
                record.valueOut?.decimalValue?.abs()
                    ?: record.mainValue?.decimalValue?.abs()
            }
            else -> record.mainValue?.decimalValue?.abs()
        }
    }

    // --- leg sync dispatch ---

    private suspend fun syncLeg1(swap: PendingMultiSwap) {
        if (swap.leg1Status == PendingMultiSwap.STATUS_EXECUTING) {
            syncLeg1Executing(swap)
        } else if (swap.leg1Status == PendingMultiSwap.STATUS_COMPLETED && swap.leg1InfoRecordUid == null) {
            backfillLeg1InfoRecordUid(swap)
        }
    }

    private suspend fun syncLeg1Executing(swap: PendingMultiSwap) {
        val updateLeg: suspend (String, BigDecimal?) -> Unit = { status, amount ->
            pendingMultiSwapStorage.updateLeg1(
                id = swap.id,
                status = status,
                amountOut = amount,
                transactionId = swap.leg1TransactionId,
            )


        }

        if (swap.leg1IsOffChain) {
            syncLegOffChain(
                providerTransactionId = swap.leg1ProviderTransactionId,
                transactionId = swap.leg1TransactionId,
                providerId = swap.leg1ProviderId,
                coinUidOut = swap.coinUidIntermediate,
                blockchainTypeOut = swap.blockchainTypeIntermediate,
                amountOut = swap.leg1AmountOut,
                legStartTime = swap.createdAt,
                updateLeg = updateLeg,
            )
        } else {
            syncLegOnChain(
                transactionId = swap.leg1TransactionId,
                coinUidIn = swap.coinUidIn,
                blockchainTypeIn = swap.blockchainTypeIn,
                coinUidOut = swap.coinUidIntermediate,
                blockchainTypeOut = swap.blockchainTypeIntermediate,
                amountOut = swap.leg1AmountOut,
                legStartTime = swap.createdAt,
                updateLeg = updateLeg,
            ) { match ->
                pendingMultiSwapStorage.setLeg1InfoRecordUid(swap.id, match.recordUid)
            }
        }
    }

    private suspend fun backfillLeg1InfoRecordUid(swap: PendingMultiSwap) {
        if (swap.leg1IsOffChain) {
            backfillLeg1InfoRecordUidOffChain(swap)
        } else {
            backfillLeg1InfoRecordUidOnChain(swap)
        }
    }

    private suspend fun backfillLeg1InfoRecordUidOffChain(swap: PendingMultiSwap) {
        val swapTx = findSwapProviderTransaction(
            providerTransactionId = swap.leg1ProviderTransactionId,
            transactionId = swap.leg1TransactionId,
            providerId = swap.leg1ProviderId,
            coinUidOut = swap.coinUidIntermediate,
            blockchainTypeOut = swap.blockchainTypeIntermediate,
            amountOut = swap.leg1AmountOut,
            legStartTime = swap.createdAt,
        ) ?: return
        val recordUid = swapTx.incomingRecordUid ?: swap.leg1TransactionId ?: return
        pendingMultiSwapStorage.setLeg1InfoRecordUid(swap.id, recordUid)
    }

    private suspend fun backfillLeg1InfoRecordUidOnChain(swap: PendingMultiSwap) {
        val match = scanIncoming(
            coinUidOut = swap.coinUidIntermediate,
            blockchainTypeOut = swap.blockchainTypeIntermediate,
            expectedAmount = swap.leg1AmountOut,
            legStartTime = swap.createdAt,
        ) ?: return
        pendingMultiSwapStorage.setLeg1InfoRecordUid(swap.id, match.recordUid)
    }

    private suspend fun syncLeg2(swap: PendingMultiSwap) {
        if (swap.leg2Status != PendingMultiSwap.STATUS_EXECUTING) return

        val updateLeg: suspend (String, BigDecimal?) -> Unit = { status, amount ->
            pendingMultiSwapStorage.updateLeg2(
                id = swap.id,
                status = status,
                amountOut = amount,
                transactionId = swap.leg2TransactionId,
            )


        }

        val isOffChain = swap.leg2IsOffChain ?: return

        if (isOffChain) {
            syncLegOffChain(
                providerTransactionId = swap.leg2ProviderTransactionId,
                transactionId = swap.leg2TransactionId,
                providerId = swap.leg2ProviderId ?: return,
                coinUidOut = swap.coinUidOut,
                blockchainTypeOut = swap.blockchainTypeOut,
                amountOut = swap.leg2AmountOut,
                legStartTime = swap.leg2StartTime(),
                updateLeg = updateLeg,
            )
        } else {
            syncLegOnChain(
                transactionId = swap.leg2TransactionId,
                coinUidIn = swap.coinUidIntermediate,
                blockchainTypeIn = swap.blockchainTypeIntermediate,
                coinUidOut = swap.coinUidOut,
                blockchainTypeOut = swap.blockchainTypeOut,
                amountOut = swap.leg2AmountOut,
                legStartTime = swap.leg2StartTime(),
                updateLeg = updateLeg,
            )
        }
    }

    // --- terminal state & cleanup ---

    private suspend fun deleteCompleted() {
        val swaps = pendingMultiSwapStorage.getAllOnce()
        swaps.forEach { swap ->
            if (swap.isTerminal()) {
                pendingMultiSwapStorage.delete(swap.id)
            }
        }
    }

    // --- helpers ---

    private fun findWallet(coinUid: String, blockchainTypeUid: String): Wallet? =
        walletManager.activeWallets.firstOrNull {
            it.coin.uid == coinUid && it.token.blockchainType == BlockchainType.fromUid(blockchainTypeUid)
        }

    private suspend fun findReceiveAddress(coinUid: String, blockchainTypeUid: String): String? {
        val wallet = findWallet(coinUid, blockchainTypeUid) ?: return null
        return adapterManager.getReceiveAddressForWallet(wallet)
    }

    private companion object {
        const val ON_CHAIN_AMOUNT_TOLERANCE = 0.10
        const val TIGHT_TIME_WINDOW_MS = 600_000L
        const val START_TIME_BUFFER_SEC = 60L
        const val MAX_PAGES = 5
        const val PAGE_SIZE = 50
    }
}
