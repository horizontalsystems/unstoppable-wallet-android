package io.horizontalsystems.bankwallet.core.adapters.zcash

import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.isFailure

class ZcashTransaction : Comparable<ZcashTransaction> {
    val id: Long
    val transactionHash: ByteArray
    val transactionIndex: Int
    val toAddress: String?
    val expiryHeight: Int?
    val minedHeight: Int
    val timestamp: Long
    val value: Long
    val memo: String?
    val failed: Boolean
    val isIncoming: Boolean

    constructor(confirmedTransaction: ConfirmedTransaction, receiveAddress: String) {
        confirmedTransaction.let {
            id = it.id
            transactionHash = it.rawTransactionId
            transactionIndex = it.transactionIndex
            toAddress = it.toAddress
            expiryHeight = it.expiryHeight
            minedHeight = it.minedHeight
            timestamp = it.blockTimeInSeconds
            value = it.value
            memo = it.memo.toUtf8Memo()
            failed = false
        }
        isIncoming = toAddress.isNullOrBlank() || toAddress == receiveAddress
    }

    constructor(pendingTransaction: PendingTransaction, receiveAddress: String) {
        pendingTransaction.let {
            id = it.id
            transactionHash = it.rawTransactionId ?: byteArrayOf()
            transactionIndex = -1
            toAddress = it.toAddress
            expiryHeight = it.expiryHeight
            minedHeight = it.minedHeight
            timestamp = it.createTime / 1000
            value = it.value
            memo = it.memo.toUtf8Memo()
            failed = it.isFailure()
        }
        isIncoming = toAddress.isNullOrBlank() || toAddress == receiveAddress
    }

    override fun equals(other: Any?): Boolean {
        return other is ZcashTransaction &&
                other.transactionHash.contentEquals(transactionHash)
    }

    override fun hashCode(): Int {
        return transactionHash.hashCode()
    }

    override fun compareTo(other: ZcashTransaction): Int = when  {
        transactionHash.contentEquals(other.transactionHash) -> 0
        timestamp == other.timestamp -> transactionIndex.compareTo(other.transactionIndex)
        else -> timestamp.compareTo(other.timestamp)
    }

    //taken from here
    //https://github.com/zcash/zcash-android-wallet/blob/371c5ef36517cab868e5345dcc8ac7517560987f/app/src/main/java/cash/z/ecc/android/ui/util/MemoUtil.kt#L24-L33
    private fun ByteArray?.toUtf8Memo(): String {
        return if (this == null || this.isEmpty() || this[0] >= 0xF5) ""
        else
            try {
                // trim empty and "replacement characters" for codes that can't be represented in unicode
                String(this, charset("UTF-8")).trim('\u0000', '\uFFFD')
            } catch (t: Throwable) {
                "Unable to parse memo."
            }
    }

}
