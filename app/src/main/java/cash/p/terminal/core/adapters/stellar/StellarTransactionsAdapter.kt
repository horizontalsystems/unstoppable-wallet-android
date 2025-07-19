package cash.p.terminal.core.adapters.stellar

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.adapters.TonTransactionsAdapter
import cash.p.terminal.core.factories.StellarTransactionConverter
import cash.p.terminal.core.managers.StellarKitWrapper
import cash.p.terminal.core.managers.toAdapterState
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.stellar.StellarTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.stellarkit.TagQuery
import io.horizontalsystems.stellarkit.room.StellarAsset
import io.horizontalsystems.stellarkit.room.Tag
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
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
        address: String?
    ): List<TransactionRecord> = try {
        val tagQuery = getTagQuery(token, transactionType, address)
        val beforeId = (from as StellarTransactionRecord?)?.operation?.id

        stellarKit.operationsBefore(tagQuery, fromId = beforeId, limit = limit)
            .map {
                transactionConverter.convert(it)
            }
    } catch (e: TonTransactionsAdapter.NotSupportedException) {
        emptyList()
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
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> Tag.Type.Incoming
            FilterTransactionType.Outgoing -> Tag.Type.Outgoing
            FilterTransactionType.Swap -> Tag.Type.Swap
            FilterTransactionType.Approve -> throw TonTransactionsAdapter.NotSupportedException()
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
        address: String?
    ): Flow<List<TransactionRecord>> = try {
        val tagQuery = getTagQuery(token, transactionType, address)
        stellarKit
            .operationFlow(tagQuery)
            .map { operationInfo ->
                operationInfo.operations.map {
                    transactionConverter.convert(it)
                }
            }
    } catch (e: TonTransactionsAdapter.NotSupportedException) {
        emptyFlow()
    }

    override fun getTransactionUrl(transactionHash: String): String {
        return "https://stellar.expert/explorer/public/tx/${transactionHash}"
    }
}