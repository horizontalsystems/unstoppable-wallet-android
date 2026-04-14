package com.quantum.wallet.bankwallet.core.adapters

import com.quantum.wallet.bankwallet.core.AdapterState
import com.quantum.wallet.bankwallet.core.ITransactionsAdapter
import com.quantum.wallet.bankwallet.core.managers.TonKitWrapper
import com.quantum.wallet.bankwallet.core.managers.toAdapterState
import com.quantum.wallet.bankwallet.entities.LastBlockInfo
import com.quantum.wallet.bankwallet.entities.transactionrecords.TransactionRecord
import com.quantum.wallet.bankwallet.modules.transactions.FilterTransactionType
import com.quantum.wallet.bankwallet.modules.transactions.FilterTransactionType.All
import com.quantum.wallet.bankwallet.modules.transactions.FilterTransactionType.Approve
import com.quantum.wallet.bankwallet.modules.transactions.FilterTransactionType.Incoming
import com.quantum.wallet.bankwallet.modules.transactions.FilterTransactionType.Outgoing
import com.quantum.wallet.bankwallet.modules.transactions.FilterTransactionType.Swap
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.Address
import io.horizontalsystems.tonkit.models.Tag
import io.horizontalsystems.tonkit.models.TagQuery
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable

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

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> = try {
        val tagQuery = getTagQuery(token, transactionType, address)
        val beforeLt = (from as TonTransactionRecord?)?.lt

        tonKit.events(tagQuery, beforeLt, limit = limit)
            .map {
                tonTransactionConverter.createTransactionRecord(it)
            }
    } catch (e: NotSupportedException) {
        listOf()
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

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> = try {
        val tagQuery = getTagQuery(token, transactionType, address)

        tonKit
            .eventFlow(tagQuery)
            .map { eventInfo ->
                eventInfo.events.map {
                    tonTransactionConverter.createTransactionRecord(it)
                }
            }

    } catch (e: NotSupportedException) {
        emptyFlow()
    }

    override fun getTransactionUrl(transactionHash: String): String {
        return "https://tonviewer.com/transaction/$transactionHash"
    }

    class NotSupportedException : Exception()
}
