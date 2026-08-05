package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.managers.ThorchainKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.thorchain.ThorchainIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.thorchain.ThorchainOutgoingTransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.thorchainkit.network.Network
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable

class ThorchainTransactionsAdapter(
    thorchainKitWrapper: ThorchainKitWrapper,
    private val transactionConverter: ThorchainTransactionConverter,
) : ITransactionsAdapter {
    private val thorchainKit = thorchainKitWrapper.thorchainKit

    private val isMaya = thorchainKit.network == Network.MayaMainnet

    override val explorerTitle = if (isMaya) "MayaScan" else "RuneScan"

    override val transactionsState: AdapterState
        get() = thorchainKit.transactionsSyncState.toAdapterState()

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = thorchainKit.transactionsSyncStateFlow.asFlowable().map {}

    override val lastBlockInfo: LastBlockInfo?
        get() = thorchainKit.lastBlockHeight.takeIf { it > 0 }?.let { LastBlockInfo(it.toInt()) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = thorchainKit.lastBlockHeightFlow.asFlowable().map {}

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> {
        return thorchainKit.getTransactions(fromTimestamp = from?.timestamp)
            .flatMap { transactionConverter.convert(it) }
            .filter { matches(it, token, transactionType, address) }
            .take(limit)
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        return thorchainKit.transactionsFlow.map { transactions ->
            transactions
                .flatMap { transactionConverter.convert(it) }
                .filter { matches(it, token, transactionType, address) }
        }
    }

    private fun matches(
        record: TransactionRecord,
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Boolean {
        val typeMatches = when (transactionType) {
            FilterTransactionType.All -> true
            FilterTransactionType.Incoming -> record is ThorchainIncomingTransactionRecord
            FilterTransactionType.Outgoing -> record is ThorchainOutgoingTransactionRecord
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> false
        }
        if (!typeMatches) return false

        if (token != null && record.mainValue?.let { it.coinUid == token.coin.uid } != true) {
            return false
        }

        if (address != null) {
            val counterparty = when (record) {
                is ThorchainIncomingTransactionRecord -> record.from
                is ThorchainOutgoingTransactionRecord -> record.to
                else -> null
            }
            if (!counterparty.equals(address, ignoreCase = true)) return false
        }

        return true
    }

    override fun getTransactionUrl(transactionHash: String): String {
        return if (isMaya) {
            "https://www.mayascan.org/tx/$transactionHash"
        } else {
            "https://runescan.io/tx/$transactionHash"
        }
    }
}
