package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.managers.ISpamOutgoingContextSource
import io.horizontalsystems.walletkit.core.managers.PoisoningScorer
import io.horizontalsystems.walletkit.core.managers.TronKitWrapper
import io.horizontalsystems.walletkit.core.managers.TronTransactionEventExtractor
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import io.horizontalsystems.walletkit.core.hexStringToByteArrayOrNull
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tronkit.TronKit
import io.horizontalsystems.tronkit.hexStringToByteArray
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.models.TransactionTag
import io.horizontalsystems.tronkit.network.Network
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TronTransactionsAdapter(
    val tronKitWrapper: TronKitWrapper,
    private val transactionConverter: TronTransactionConverter
) : ITransactionsAdapter, ISpamOutgoingContextSource {

    private val tronKit = tronKitWrapper.tronKit
    private val spamContextExtractor = TronTransactionEventExtractor()

    override val explorerTitle: String
        get() = "Tronscan"

    override fun getTransactionUrl(transactionHash: String): String = when (tronKit.network) {
        Network.Mainnet -> "https://tronscan.io/#/transaction/$transactionHash"
        Network.ShastaTestnet -> "https://shasta.tronscan.org/#/transaction/$transactionHash"
        Network.NileTestnet -> "https://nile.tronscan.org/#/transaction/$transactionHash"
    }

    override val lastBlockInfo: LastBlockInfo
        get() = tronKit.lastBlockHeight.toInt().let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlow: Flow<Unit>
        get() = tronKit.lastBlockHeightFlow.map { }

    override val transactionsState: AdapterState
        get() = convertToAdapterState(tronKit.transactionsSyncState)

    override val transactionsStateUpdatedFlow: Flow<Unit>
        get() = (tronKit.transactionsSyncStateFlow ?: tronKit.syncStateFlow).map {}

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> {
        return tronKit.getFullTransactionsBefore(
            getFilters(token, transactionType, address),
            from?.transactionHash?.hexStringToByteArray(),
            limit
        ).map {
            transactionConverter.transactionRecord(it)
        }
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        return tronKit.getFullTransactionsFlow(getFilters(token, transactionType, address))
            .map { transactions ->
                transactions.map { transactionConverter.transactionRecord(it) }
            }
    }

    override suspend fun getTransactionsAfter(fromTransactionId: String?): List<TransactionRecord> {
        return tronKit.getFullTransactionsAfter(listOf(), fromTransactionId?.hexStringToByteArrayOrNull())
            .map {
                transactionConverter.transactionRecord(it)
            }
    }

    override suspend fun getOutgoingContext(
        transactionHash: ByteArray,
        operationId: Long?,
        limit: Int
    ): List<PoisoningScorer.OutgoingTxInfo> {
        val userAddress = tronKit.address
        return tronKit.getFullTransactionsBefore(listOf(), transactionHash, limit)
            .sortedByDescending { it.transaction.timestamp }
            .mapNotNull { spamContextExtractor.extractCounterpartyInfo(it, userAddress) }
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
