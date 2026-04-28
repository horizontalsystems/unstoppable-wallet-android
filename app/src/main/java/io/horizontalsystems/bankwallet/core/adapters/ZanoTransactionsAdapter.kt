package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.zano.ZanoIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.zano.ZanoOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.zanokit.TransactionInfo
import io.horizontalsystems.zanokit.TransactionType
import io.horizontalsystems.zanokit.ZanoKit
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import java.math.BigDecimal

class ZanoTransactionsAdapter(
    private val kit: ZanoKit,
    private val transactionsProvider: ZanoTransactionsProvider,
    private val wallet: Wallet,
) : ITransactionsAdapter {

    override val explorerTitle: String = "Zano Explorer"

    override val transactionsState: AdapterState
        get() = kit.syncStateFlow.value.toAdapterState()

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = kit.syncStateFlow.asFlowable().map { }

    override val lastBlockInfo: LastBlockInfo?
        get() = null

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = Flowable.empty()

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?
    ): List<TransactionRecord> =
        transactionsProvider.getTransactions(from?.uid, transactionType, limit)
            .map { toRecord(it) }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?
    ): Flow<List<TransactionRecord>> =
        transactionsProvider.getNewTransactionsFlow(transactionType)
            .map { txs -> txs.map { toRecord(it) } }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://explorer.zano.org/transaction/$transactionHash"

    private fun toRecord(tx: TransactionInfo): TransactionRecord {
        val blockHeight = if (tx.isPending || tx.blockHeight == 0L) null else tx.blockHeight.toInt()
        val amount = tx.amount.scaledDown(DECIMALS)
        val fee = tx.fee.scaledDown(DECIMALS).takeIf { it > BigDecimal.ZERO }

        return when (tx.type) {
            TransactionType.incoming -> ZanoIncomingTransactionRecord(
                token = wallet.token,
                uid = tx.uid,
                transactionHash = tx.hash,
                transactionIndex = 0,
                blockHeight = blockHeight,
                confirmationsThreshold = ZanoKit.CONFIRMATIONS_THRESHOLD,
                timestamp = tx.timestamp,
                fee = fee,
                failed = tx.isFailed,
                amount = amount,
                from = tx.recipientAddress,
                memo = tx.memo,
                source = wallet.transactionSource
            )
            TransactionType.outgoing,
            TransactionType.sentToSelf -> ZanoOutgoingTransactionRecord(
                token = wallet.token,
                uid = tx.uid,
                transactionHash = tx.hash,
                transactionIndex = 0,
                blockHeight = blockHeight,
                confirmationsThreshold = ZanoKit.CONFIRMATIONS_THRESHOLD,
                timestamp = tx.timestamp,
                fee = fee,
                failed = tx.isFailed,
                amount = amount.negate(),
                to = tx.recipientAddress,
                sentToSelf = tx.type == TransactionType.sentToSelf,
                memo = tx.memo,
                source = wallet.transactionSource
            )
        }
    }

    companion object {
        const val DECIMALS = 12
    }
}
