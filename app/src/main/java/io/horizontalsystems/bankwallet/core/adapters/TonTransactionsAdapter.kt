package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.TonKitWrapper
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tonkit.models.Tag
import io.horizontalsystems.tonkit.models.TagQuery
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.rxSingle

class TonTransactionsAdapter(
    tonKitWrapper: TonKitWrapper,
    private val tonTransactionConverter: TonTransactionConverter,
) : ITransactionsAdapter {

    private val tonKit = tonKitWrapper.tonKit

    override val explorerTitle = "tonscan.org"
    override var transactionsState: AdapterState = AdapterState.Syncing()
    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = tonKit.eventSyncStateFlow.asFlowable().map {}
    override val lastBlockInfo: LastBlockInfo?
        get() = null
    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = Flowable.empty()

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): Single<List<TransactionRecord>> {
        val beforeLt = (from as TonTransactionRecord?)?.logicalTime
        val tagQuery = TagQuery(null, Tag.Platform.Native, null, null)

        return rxSingle {
            tonKit.events(tagQuery, beforeLt, limit = limit)
                .map {
                    tonTransactionConverter.createTransactionRecord(it)
                }
        }
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flowable<List<TransactionRecord>> {
        return Flowable.empty()
    }

    override fun getTransactionUrl(transactionHash: String): String {
        return "https://tonscan.org/tx/$transactionHash"
    }
}
