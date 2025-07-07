package cash.p.terminal.core.adapters

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.MoneroKitWrapper
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.monero.MoneroTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import com.m2049r.xmrwallet.model.TransactionInfo
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import java.math.BigDecimal

class MoneroTransactionsAdapter(
    private val moneroKitWrapper: MoneroKitWrapper,
    private val source: TransactionSource
) : ITransactionsAdapter {

    override val explorerTitle: String
        get() = "Monero Explorer"

    override fun getTransactionUrl(transactionHash: String): String =
        "https://localmonero.co/blocks/tx/$transactionHash"

    override val lastBlockInfo: LastBlockInfo?
        get() = moneroKitWrapper.lastBlockInfoFlow.value

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = moneroKitWrapper.lastBlockInfoFlow.map {}.asFlowable()

    override val transactionsState: AdapterState
        get() = AdapterState.Synced

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = moneroKitWrapper.transactionsStateUpdatedFlow.asFlowable()

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> {
        val token = token ?: return emptyList()

        val moneroTransactions = moneroKitWrapper.getTransactions()

        val filteredByType = when (transactionType) {
            FilterTransactionType.All -> moneroTransactions
            FilterTransactionType.Incoming -> moneroTransactions.filter {
                it.direction == TransactionInfo.Direction.Direction_In
            }

            FilterTransactionType.Outgoing -> moneroTransactions.filter {
                it.direction == TransactionInfo.Direction.Direction_Out
            }

            // We don't support swaps and approve in Monero
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> return emptyList()
        }

        val filteredByAddress = if (address != null) {
            filteredByType.filter { transactionInfo ->
                transactionInfo.address == address ||
                        transactionInfo.transfers?.any { it.address == address } == true
            }
        } else {
            filteredByType
        }

        val sortedTransactions = filteredByAddress.sortedByDescending { it.timestamp }

        val startIndex = if (from != null) {
            val fromIndex = sortedTransactions.indexOfFirst { it.hash == from.transactionHash }
            if (fromIndex >= 0) fromIndex + 1 else 0
        } else {
            0
        }

        val endIndex = minOf(startIndex + limit, sortedTransactions.size)
        val pagedTransactions = sortedTransactions.subList(startIndex, endIndex)

        return pagedTransactions.map { transactionInfo ->
            convertToTransactionRecord(transactionInfo, token)
        }
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> = moneroKitWrapper.transactionsStateUpdatedFlow.map {
        getTransactions(null, token, 100, transactionType, address)
    }

    private fun convertToTransactionRecord(
        transactionInfo: TransactionInfo,
        token: Token
    ): TransactionRecord {
        val recordType = when (transactionInfo.direction) {
            TransactionInfo.Direction.Direction_In -> TransactionRecordType.MONERO_INCOMING
            TransactionInfo.Direction.Direction_Out -> TransactionRecordType.MONERO_OUTGOING
        }

        val (toAddress, fromAddress) = when (transactionInfo.direction) {
            TransactionInfo.Direction.Direction_In -> {
                val ourAddress = transactionInfo.address
                val senderAddress = transactionInfo.transfers?.firstOrNull()?.address
                Pair(ourAddress, senderAddress)
            }

            TransactionInfo.Direction.Direction_Out -> {
                val ourAddress = transactionInfo.address
                val recipientAddress = transactionInfo.transfers?.firstOrNull()?.address
                Pair(recipientAddress, ourAddress)
            }
        }

        val sentToSelf = transactionInfo.direction == TransactionInfo.Direction.Direction_Out &&
                transactionInfo.transfers?.all { it.address == transactionInfo.address } == true
        val negative = transactionInfo.direction == TransactionInfo.Direction.Direction_Out

        return MoneroTransactionRecord(
            uid = transactionInfo.hash,
            transactionHash = transactionInfo.hash,
            blockHeight = if (transactionInfo.blockheight > 0) transactionInfo.blockheight.toInt() else null,
            confirmationsThreshold = TransactionInfo.CONFIRMATION,
            timestamp = transactionInfo.timestamp,
            failed = transactionInfo.isFailed,
            transactionRecordType = recordType,
            token = token,
            to = toAddress,
            from = fromAddress,
            sentToSelf = sentToSelf,
            memo = transactionInfo.notes,
            amount = convertAmount(transactionInfo.amount, token.decimals, negative),
            fee = TransactionValue.CoinValue(
                token,
                convertAmount(transactionInfo.fee, token.decimals, false)
            ),
            subaddressLabel = transactionInfo.subaddressLabel,
            isPending = transactionInfo.isPending,
            confirmations = transactionInfo.confirmations,
            source = source
        )
    }

    private fun convertAmount(amount: Long, decimal: Int, negative: Boolean): BigDecimal {
        var significandAmount = amount.toBigDecimal().movePointLeft(decimal).stripTrailingZeros()

        if (significandAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO
        }

        if (negative) {
            significandAmount = significandAmount.negate()
        }

        return significandAmount
    }
} 