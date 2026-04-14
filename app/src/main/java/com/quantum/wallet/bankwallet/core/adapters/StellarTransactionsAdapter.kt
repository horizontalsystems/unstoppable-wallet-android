package com.quantum.wallet.bankwallet.core.adapters

import com.quantum.wallet.bankwallet.core.AdapterState
import com.quantum.wallet.bankwallet.core.ITransactionsAdapter
import com.quantum.wallet.bankwallet.core.adapters.TonTransactionsAdapter.NotSupportedException
import com.quantum.wallet.bankwallet.core.factories.StellarTransactionConverter
import com.quantum.wallet.bankwallet.core.managers.StellarKitWrapper
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
import io.horizontalsystems.stellarkit.TagQuery
import io.horizontalsystems.stellarkit.room.StellarAsset
import io.horizontalsystems.stellarkit.room.Tag
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable

class StellarTransactionsAdapter(
    val stellarKitWrapper: StellarKitWrapper,
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
                    transactionConverter.convert(it)
                }
            }
    } catch (e: NotSupportedException) {
        emptyFlow()
    }

    override fun getTransactionUrl(transactionHash: String): String {
        return "https://stellar.expert/explorer/public/tx/${transactionHash}"
    }

    override suspend fun getStellarOperationsBefore(
        fromId: Long?,
        limit: Int
    ): List<io.horizontalsystems.stellarkit.room.Operation> {
        return stellarKit.operationsBefore(TagQuery(null, null, null), fromId = fromId, limit = limit)
    }
}