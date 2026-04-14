package com.quantum.wallet.bankwallet.entities.transactionrecords

import com.quantum.wallet.bankwallet.entities.LastBlockInfo
import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.entities.nft.NftUid
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import com.quantum.wallet.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.marketkit.models.BlockchainType

abstract class TransactionRecord(
    val uid: String,
    val transactionHash: String,
    val transactionIndex: Int,
    val blockHeight: Int?,
    val confirmationsThreshold: Int?,
    val timestamp: Long,
    val failed: Boolean = false,
    val spam: Boolean = false,
    val source: TransactionSource
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
            else -> uid.compareTo(other.uid)
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