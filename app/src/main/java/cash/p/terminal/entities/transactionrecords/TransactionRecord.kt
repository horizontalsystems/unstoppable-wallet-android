package cash.p.terminal.entities.transactionrecords

import cash.p.terminal.core.adapters.TonTransactionRecord
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.ContractCallTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import cash.p.terminal.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.BlockchainType

abstract class TransactionRecord(
    val uid: String,
    val transactionHash: String,
    val transactionIndex: Int,
    val blockHeight: Int?,
    val confirmationsThreshold: Int?,
    val timestamp: Long,
    val failed: Boolean = false,
    val spam: Boolean = false,
    val source: TransactionSource,
    val transactionRecordType: TransactionRecordType
) : Comparable<TransactionRecord> {

    open val mainValue: TransactionValue? = null

    val blockchainType: BlockchainType
        get() = source.blockchain.type

    open fun changedBy(oldBlockInfo: LastBlockInfo?, newBlockInfo: LastBlockInfo?): Boolean =
        status(oldBlockInfo?.height) != status(newBlockInfo?.height)

    override fun compareTo(other: TransactionRecord): Int {
        return when {
            timestamp != other.timestamp -> timestamp.compareTo(other.timestamp)
            transactionIndex != other.transactionIndex -> transactionIndex.compareTo(other.transactionIndex)
            else -> uid.compareTo(uid)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is TransactionRecord) {
            return uid == other.uid
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    open fun status(lastBlockHeight: Int?): TransactionStatus {
        if (failed) {
            return TransactionStatus.Failed
        } else if (blockHeight != null && lastBlockHeight != null) {
            val threshold = confirmationsThreshold ?: 1
            val confirmations = lastBlockHeight - blockHeight.toInt() + 1

            return if (confirmations >= threshold) {
                TransactionStatus.Completed
            } else {
                TransactionStatus.Processing(confirmations.toFloat() / threshold.toFloat())
            }
        }

        return TransactionStatus.Pending
    }
}

val TransactionRecord.nftUids: Set<NftUid>
    get() = when (this) {
        is EvmOutgoingTransactionRecord -> {
            value.nftUid?.let { setOf(it) } ?: emptySet()
        }

        is ContractCallTransactionRecord -> {
            ((incomingEvents + outgoingEvents).mapNotNull { it.value.nftUid }).toSet()
        }

        is ExternalContractCallTransactionRecord -> {
            ((incomingEvents + outgoingEvents).mapNotNull { it.value.nftUid }).toSet()
        }

        else -> emptySet()
    }

val List<TransactionRecord>.nftUids: Set<NftUid>
    get() {
        val nftUids = mutableSetOf<NftUid>()
        forEach { nftUids.addAll(it.nftUids) }
        return nftUids
    }

fun TransactionRecord.getShortOutgoingTransactionRecord(): ShortOutgoingTransactionRecord? = when (this) {
    is BinanceChainOutgoingTransactionRecord ->
        ShortOutgoingTransactionRecord(
            amountOut = mainValue.decimalValue.abs(),
            token = value.token,
            timestamp = timestamp * 1000
        )

    is BitcoinOutgoingTransactionRecord ->
        ShortOutgoingTransactionRecord(
            amountOut = mainValue.decimalValue?.abs(),
            token = token,
            timestamp = timestamp * 1000
        )

    is EvmOutgoingTransactionRecord ->
        ShortOutgoingTransactionRecord(
            amountOut = mainValue.decimalValue?.abs(),
            token = (mainValue as? TransactionValue.CoinValue)?.token,
            timestamp = timestamp * 1000
        )

    is SolanaOutgoingTransactionRecord ->
        ShortOutgoingTransactionRecord(
            amountOut = mainValue.decimalValue?.abs(),
            token = baseToken,
            timestamp = timestamp * 1000
        )

    is TronOutgoingTransactionRecord ->
        ShortOutgoingTransactionRecord(
            amountOut = mainValue.decimalValue?.abs(),
            token = baseToken,
            timestamp = timestamp * 1000
        )

    is TonTransactionRecord ->
        if (actions.singleOrNull()?.type is TonTransactionRecord.Action.Type.Send) {
            ShortOutgoingTransactionRecord(
                amountOut = this.mainValue?.decimalValue?.abs(),
                token = baseToken,
                timestamp = timestamp * 1000
            )
        } else {
            null
        }

    else -> null
}
