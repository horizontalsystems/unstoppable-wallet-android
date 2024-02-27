package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.TronKitWrapper
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tronkit.TronKit
import io.horizontalsystems.tronkit.hexStringToByteArray
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.models.TransactionTag
import io.horizontalsystems.tronkit.network.Network
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.rxSingle

class TronTransactionsAdapter(
    val tronKitWrapper: TronKitWrapper,
    private val transactionConverter: TronTransactionConverter
) : ITransactionsAdapter {

    private val tronKit = tronKitWrapper.tronKit

    override val explorerTitle: String
        get() = "Tronscan"

    override fun getTransactionUrl(transactionHash: String): String = when (tronKit.network) {
        Network.Mainnet -> "https://tronscan.io/#/transaction/$transactionHash"
        Network.ShastaTestnet -> "https://shasta.tronscan.org/#/transaction/$transactionHash"
        Network.NileTestnet -> "https://nile.tronscan.org/#/transaction/$transactionHash"
    }

    override val lastBlockInfo: LastBlockInfo
        get() = tronKit.lastBlockHeight.toInt().let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = tronKit.lastBlockHeightFlow.asFlowable().map { }

    override val transactionsState: AdapterState
        get() = convertToAdapterState(tronKit.syncState)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = tronKit.syncStateFlow.asFlowable().map {}

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): Single<List<TransactionRecord>> {
        return rxSingle {
            tronKit.getFullTransactions(
                getFilters(token, transactionType, address),
                from?.transactionHash?.hexStringToByteArray(),
                limit
            ).map {
                transactionConverter.transactionRecord(it)
            }
        }
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flowable<List<TransactionRecord>> {
        return tronKit.getFullTransactionsFlow(getFilters(token, transactionType, address))
            .map { transactions ->
                transactions.map { transactionConverter.transactionRecord(it) }
            }
            .asFlowable()
    }

    private fun convertToAdapterState(syncState: TronKit.SyncState): AdapterState =
        when (syncState) {
            is TronKit.SyncState.Synced -> AdapterState.Synced
            is TronKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is TronKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    private fun coinTagName(token: Token) = when (val type = token.type) {
        TokenType.Native -> TransactionTag.TRX_COIN
        is TokenType.Eip20 -> type.address
        else -> ""
    }

    private fun incomingTag(token: Token) = when (val type = token.type) {
        TokenType.Native -> TransactionTag.TRX_COIN_INCOMING
        is TokenType.Eip20 -> TransactionTag.trc20Incoming(type.address)
        else -> ""
    }

    private fun outgoingTag(token: Token) = when (val type = token.type) {
        TokenType.Native -> TransactionTag.TRX_COIN_OUTGOING
        is TokenType.Eip20 -> TransactionTag.trc20Outgoing(type.address)
        else -> ""
    }

    private fun getFilters(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ) = buildList {
        token?.let {
            add(listOf(coinTagName(it)))
        }

        val filterType = when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> when {
                token != null -> incomingTag(token)
                else -> TransactionTag.INCOMING
            }
            FilterTransactionType.Outgoing -> when {
                token != null -> outgoingTag(token)
                else -> TransactionTag.OUTGOING
            }
            FilterTransactionType.Swap -> TransactionTag.SWAP
            FilterTransactionType.Approve -> TransactionTag.TRC20_APPROVE
        }

        filterType?.let {
            add(listOf(it))
        }

        val addressHex = address?.let { Address.fromBase58(it).hex }?.lowercase()
        if (!addressHex.isNullOrBlank()) {
            add(listOf("from_$addressHex", "to_$addressHex"))
        }
    }
}
