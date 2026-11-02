package io.horizontalsystems.walletkit.entities.transactionrecords.mweb

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

/**
 * A Litecoin MWEB history row. MWEB-to-MWEB transactions have no Litecoin
 * transaction hash; [transactionHash] then carries the record uid so the row
 * still has a stable identity. [litecoinTxHash] is set for peg-ins and
 * peg-outs only.
 *
 * [blockTimestamp] is null for an output recovered from a block below the
 * kit's header checkpoint: the light client knows its height but no block
 * time. Such rows sort as the oldest, by height between themselves, since
 * the base class orders by timestamp and then [transactionIndex].
 */
abstract class MwebTransactionRecord(
    uid: String,
    transactionHash: String,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    val blockTimestamp: Long?,
    val fee: TransactionValue?,
    failed: Boolean,
    val litecoinTxHash: String?,
    source: TransactionSource,
) : TransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    transactionIndex = blockHeight ?: 0,
    blockHeight = blockHeight,
    confirmationsThreshold = confirmationsThreshold,
    timestamp = blockTimestamp ?: 0,
    failed = failed,
    source = source,
)

class MwebIncomingTransactionRecord(
    token: Token,
    uid: String,
    transactionHash: String,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    timestamp: Long?,
    fee: BigDecimal?,
    failed: Boolean,
    amount: BigDecimal,
    /** True when the coins arrived from the Litecoin side through a peg-in. */
    val pegIn: Boolean,
    litecoinTxHash: String?,
    source: TransactionSource,
) : MwebTransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    blockHeight = blockHeight,
    confirmationsThreshold = confirmationsThreshold,
    blockTimestamp = timestamp,
    fee = fee?.let { TransactionValue.CoinValue(token, it) },
    failed = failed,
    litecoinTxHash = litecoinTxHash,
    source = source,
) {
    val value: TransactionValue = TransactionValue.CoinValue(token, amount)

    override val mainValue = value
}

class MwebOutgoingTransactionRecord(
    token: Token,
    uid: String,
    transactionHash: String,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    timestamp: Long?,
    fee: BigDecimal?,
    failed: Boolean,
    amount: BigDecimal,
    val to: String?,
    /** True when the coins left MWEB for a Litecoin address. */
    val pegOut: Boolean,
    /** True when an output left the unspent set without a send recorded here (another wallet on the same seed). */
    val spentElsewhere: Boolean,
    litecoinTxHash: String?,
    source: TransactionSource,
) : MwebTransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    blockHeight = blockHeight,
    confirmationsThreshold = confirmationsThreshold,
    blockTimestamp = timestamp,
    fee = fee?.let { TransactionValue.CoinValue(token, it) },
    failed = failed,
    litecoinTxHash = litecoinTxHash,
    source = source,
) {
    val value: TransactionValue = TransactionValue.CoinValue(token, amount)

    override val mainValue = value
}
