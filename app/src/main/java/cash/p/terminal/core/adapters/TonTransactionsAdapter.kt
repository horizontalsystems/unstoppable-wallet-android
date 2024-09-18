package cash.p.terminal.core.adapters

import cash.p.terminal.core.AdapterState
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.TonKitWrapper
import cash.p.terminal.core.managers.toAdapterState
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.modules.transactions.FilterTransactionType.All
import cash.p.terminal.modules.transactions.FilterTransactionType.Approve
import cash.p.terminal.modules.transactions.FilterTransactionType.Incoming
import cash.p.terminal.modules.transactions.FilterTransactionType.Outgoing
import cash.p.terminal.modules.transactions.FilterTransactionType.Swap
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.Address
import io.horizontalsystems.tonkit.models.Tag
import io.horizontalsystems.tonkit.models.TagQuery
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.rxSingle

class TonTransactionsAdapter(
    tonKitWrapper: TonKitWrapper,
    private val tonTransactionConverter: TonTransactionConverter,
) : ITransactionsAdapter {

    private val tonKit = tonKitWrapper.tonKit

    override val explorerTitle = "tonviewer.com"
    override val transactionsState: AdapterState
        get() = tonKit.eventSyncStateFlow.value.toAdapterState()
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
    ): Single<List<TransactionRecord>> = try {
        val tagQuery = getTagQuery(token, transactionType, address)
        val beforeLt = (from as TonTransactionRecord?)?.lt

        rxSingle {
            tonKit.events(tagQuery, beforeLt, limit = limit)
                .map {
                    tonTransactionConverter.createTransactionRecord(it)
                }
        }
    } catch (e: NotSupportedException) {
        Single.just(listOf())
    }

    private fun getTagQuery(token: Token?, transactionType: FilterTransactionType, address: String?): TagQuery {
        var platform: Tag.Platform? = null
        var jettonAddress: Address? = null

        val tokenType = token?.type

        if (tokenType == TokenType.Native) {
            platform = Tag.Platform.Native
        } else if (tokenType is TokenType.Jetton) {
            platform = Tag.Platform.Jetton
            jettonAddress = Address.parse(tokenType.address)
        }

        val tagType = when (transactionType) {
            All -> null
            Incoming -> Tag.Type.Incoming
            Outgoing -> Tag.Type.Outgoing
            Swap -> Tag.Type.Swap
            Approve -> throw NotSupportedException()
        }

        return TagQuery(
            tagType,
            platform,
            jettonAddress,
            address?.let { Address.parse(it) }
        )
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flowable<List<TransactionRecord>> = try {
        val tagQuery = getTagQuery(token, transactionType, address)

        tonKit
            .eventFlow(tagQuery)
            .map { eventInfo ->
                eventInfo.events.map {
                    tonTransactionConverter.createTransactionRecord(it)
                }
            }
            .asFlowable()

    } catch (e: NotSupportedException) {
        Flowable.empty()
    }

    override fun getTransactionUrl(transactionHash: String): String {
        return "https://tonviewer.com/transaction/$transactionHash"
    }

    class NotSupportedException : Exception()
}
