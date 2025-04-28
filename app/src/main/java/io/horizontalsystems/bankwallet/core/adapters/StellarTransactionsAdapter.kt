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
import io.reactivex.Single
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.rxSingle

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

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): Single<List<TransactionRecord>> = try {
        val tagQuery = getTagQuery(token, transactionType, address)
        val beforeId = (from as StellarTransactionRecord?)?.operation?.id

        rxSingle {
            stellarKit.operations(tagQuery, beforeId = beforeId, limit = limit)
                .map {
                    transactionConverter.convert(it)
                }
        }
    } catch (e: NotSupportedException) {
        Single.just(listOf())
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

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flowable<List<TransactionRecord>> {
        return Flowable.empty()
    }

    override fun getTransactionUrl(transactionHash: String): String {
        return "https://stellar.expert/explorer/public/tx/${transactionHash}"
    }
}