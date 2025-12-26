package io.horizontalsystems.bankwallet.core.adapters.zcash

import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.TransactionState
import cash.z.ecc.android.sdk.model.Zatoshi
import java.util.Date

class ZcashTransaction : Comparable<ZcashTransaction> {
    val rawId: FirstClassByteArray
    val transactionHash: ByteArray
    val transactionIndex: Int
    val recipients: List<TransactionRecipient>?
    val expiryHeight: Int?
    val minedHeight: Long?
    val timestamp: Long
    val value: Zatoshi
    val feePaid: Zatoshi?
    val memo: String?
    val failed: Boolean
    val isIncoming: Boolean
    val shieldDirection: ShieldDirection?

    constructor(accountId: AccountUuid, confirmedTransaction: TransactionOverview, recipients: List<TransactionRecipient>?, memo: String?) {
        confirmedTransaction.let {
            val hasSpentAndReceived = it.totalSpent.value > 0 && it.totalReceived.value > 0

            val internalTransaction = hasSpentAndReceived &&
                    !recipients.isNullOrEmpty() &&
                    it.isSentTransaction &&
                    recipients.all { recipient -> recipient.accountUuid == accountId }

            if (it.isShielding || internalTransaction) {
                shieldDirection = if (it.isShielding) ShieldDirection.Shield else ShieldDirection.Unshield
                feePaid = it.totalSpent - it.totalReceived
                value = it.totalReceived
            } else {
                shieldDirection = null
                feePaid = it.feePaid
                value = it.netValue
            }

            rawId = it.txId.value
            transactionHash = it.txId.value.byteArray
            transactionIndex = it.index?.toInt() ?: -1
            this.recipients = recipients
            expiryHeight = it.expiryHeight?.value?.toInt()
            minedHeight = it.minedHeight?.value
            timestamp = it.blockTimeEpochSeconds ?: when (it.transactionState) {
                TransactionState.Pending -> Date().time / 1000
                else -> 0
            }
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

    override fun compareTo(other: ZcashTransaction): Int = when {
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

    enum class ShieldDirection {
        Shield, Unshield
    }

}
