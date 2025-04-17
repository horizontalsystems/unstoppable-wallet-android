package cash.p.terminal.entities.transactionrecords.bitcoin

import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.modules.transactions.TransactionLockInfo
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import java.math.BigDecimal
import java.util.*

class BitcoinTransactionRecord(
    token: Token,
    amount: BigDecimal,
    to: String?,
    from: String?,
    uid: String,
    transactionHash: String,
    transactionIndex: Int,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    timestamp: Long,
    failed: Boolean,
    memo: String?,
    source: TransactionSource,
    sentToSelf: Boolean = false,
    transactionRecordType: TransactionRecordType,
    val fee: TransactionValue?,
    val lockInfo: TransactionLockInfo?,
    val conflictingHash: String?,
    val showRawTransaction: Boolean,
    val replaceable: Boolean = false,
    override val mainValue: TransactionValue = TransactionValue.CoinValue(token, amount),
) : TransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    transactionIndex = transactionIndex,
    blockHeight = blockHeight,
    confirmationsThreshold = confirmationsThreshold,
    timestamp = timestamp,
    failed = failed,
    source = source,
    transactionRecordType = transactionRecordType,
    token = token,
    to = to,
    from = from,
    memo = memo,
    sentToSelf = sentToSelf
) {

    override fun changedBy(oldBlockInfo: LastBlockInfo?, newBlockInfo: LastBlockInfo?): Boolean {
        return super.changedBy(oldBlockInfo, newBlockInfo)
                || becomesUnlocked(oldBlockInfo?.timestamp, newBlockInfo?.timestamp)
    }

    fun lockState(lastBlockTimestamp: Long?): TransactionLockState? {
        val lockInfo = lockInfo ?: return null

        var locked = true

        lastBlockTimestamp?.let {
            locked = it < lockInfo.lockedUntil.time / 1000
        }

        return TransactionLockState(locked, lockInfo.lockedUntil)
    }

    private fun becomesUnlocked(oldTimestamp: Long?, newTimestamp: Long?): Boolean {
        val lockTime = lockInfo?.lockedUntil?.time?.div(1000) ?: return false
        newTimestamp ?: return false

        return lockTime > (oldTimestamp ?: 0L) && // was locked
                lockTime <= newTimestamp       // now unlocked
    }
}

data class TransactionLockState(val locked: Boolean, val date: Date)
