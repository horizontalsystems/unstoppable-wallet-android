package io.horizontalsystems.bankwallet.core.adapters.zcash

import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionState
import cash.z.ecc.android.sdk.model.Zatoshi
import java.util.Date

class ZcashTransaction : Comparable<ZcashTransaction> {
    val rawId: FirstClassByteArray
    val transactionHash: ByteArray
    val transactionIndex: Int
    val toAddress: String?
    val expiryHeight: Int?
    val minedHeight: Long?
    val timestamp: Long
    val value: Zatoshi
    val feePaid: Zatoshi?
    val memo: String?
    val failed: Boolean
    val isIncoming: Boolean

    constructor(confirmedTransaction: TransactionOverview, recipient: String?, memo: String?) {
        confirmedTransaction.let {
            rawId = it.rawId
            transactionHash = it.rawId.byteArray
            transactionIndex = it.index?.toInt() ?: -1
            toAddress = recipient
            expiryHeight = it.expiryHeight?.value?.toInt()
            minedHeight = it.minedHeight?.value
            timestamp = it.blockTimeEpochSeconds ?: when (it.transactionState) {
                TransactionState.Pending -> Date().time / 1000
                else -> 0
            }
            value = it.netValue
            feePaid = it.feePaid
            this.memo = memo
            failed = false
            isIncoming = !it.isSentTransaction
        }
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
