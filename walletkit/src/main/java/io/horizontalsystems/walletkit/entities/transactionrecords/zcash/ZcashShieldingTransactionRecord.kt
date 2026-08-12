package io.horizontalsystems.walletkit.entities.transactionrecords.zcash

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.TransactionLockInfo
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class ZcashShieldingTransactionRecord(
    token: Token,
    source: TransactionSource,
    uid: String,
    transactionHash: String,
    transactionIndex: Int,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    timestamp: Long,
    fee: BigDecimal?,
    failed: Boolean,
    lockInfo: TransactionLockInfo?,
    conflictingHash: String?,
    showRawTransaction: Boolean,
    amount: BigDecimal,
    val direction: Direction,
    memo: String? = null
) : BitcoinTransactionRecord(
    source = source,
    uid = uid,
    transactionHash = transactionHash,
    transactionIndex = transactionIndex,
    blockHeight = blockHeight,
    confirmationsThreshold = confirmationsThreshold,
    timestamp = timestamp,
    fee = fee?.let { TransactionValue.CoinValue(token, it) },
    failed = failed,
    lockInfo = lockInfo,
    conflictingHash = conflictingHash,
    showRawTransaction = showRawTransaction,
    memo = memo
) {
    val value: TransactionValue = TransactionValue.CoinValue(token, amount)

    override val mainValue = value

    enum class Direction(val title: Int, val icon: Int) {
        Shield(R.string.Transactions_Shield, R.drawable.ic_shield_24),
        Unshield(R.string.Transactions_Unshield, R.drawable.ic_shield_off_24),
        MigrateToIronwood(R.string.Transactions_Migrate, R.drawable.ic_migrate_24);

        companion object {
            fun from(wrapperDirection: ShieldDirection): Direction {
                return when (wrapperDirection) {
                    ShieldDirection.Shield -> Shield
                    ShieldDirection.Unshield -> Unshield
                    ShieldDirection.MigrateToIronwood -> MigrateToIronwood
                }
            }
        }
    }
}

enum class ShieldDirection {
    Shield, Unshield, MigrateToIronwood
}
