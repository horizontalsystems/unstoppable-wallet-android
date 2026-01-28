package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.TonTransactionsAdapter.NotSupportedException
import io.horizontalsystems.bankwallet.core.factories.StellarTransactionConverter
import io.horizontalsystems.bankwallet.core.managers.StellarKitWrapper
import io.horizontalsystems.bankwallet.core.managers.toAdapterState
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType.All
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType.Approve
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType.Incoming
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType.Outgoing
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType.Swap
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.TagQuery
import io.horizontalsystems.stellarkit.room.StellarAsset
import io.horizontalsystems.stellarkit.room.Tag
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asFlowable

class StellarTransactionsAdapter(
    stellarKitWrapper: StellarKitWrapper,
    private val transactionConverter: StellarTransactionConverter,
) : ITransactionsAdapter {
    private val stellarKit = stellarKitWrapper.stellarKit

    override val explorerTitle = "Stellar Expert"
    override val transactionsState: AdapterState
        get() = stellarKit.operationsSyncStateFlow.value.toAdapterState()
    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = stellarKit.operationsSyncStateFlow.asFlowable().map {}
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
        val beforeId = (from as StellarTransactionRecord?)?.operation?.id

        stellarKit.operationsBefore(tagQuery, fromId = beforeId, limit = limit)
            .map {
                transactionConverter.convert(it)
            }
    } catch (e: NotSupportedException) {
        listOf()
    }

    override suspend fun getTransactionsAfter(fromTransactionId: String?): List<TransactionRecord> {
        return stellarKit.operationsAfter(TagQuery(null, null, null), fromTransactionId?.toLongOrNull(), 10000)
            .map {
                transactionConverter.convert(it)
            }
    }

    private fun getTagQuery(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): TagQuery {
        var assetId: String? = null

        val tokenType = token?.type

        if (tokenType == TokenType.Native) {
            assetId = StellarAsset.Native.id
        } else if (tokenType is TokenType.Asset) {
            assetId = StellarAsset.Asset(tokenType.code, tokenType.issuer).id
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
            assetId,
            address
        )
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> = try {
        val tagQuery = getTagQuery(token, transactionType, address)

        stellarKit
            .operationFlow(tagQuery)
            .map { operationInfo ->
                operationInfo.operations.map {
                    runBlocking { transactionConverter.convert(it) }
                }
            }
    } catch (e: NotSupportedException) {
        emptyFlow()
    }

    override fun getTransactionUrl(transactionHash: String): String {
        return "https://stellar.expert/explorer/public/tx/${transactionHash}"
    }
}