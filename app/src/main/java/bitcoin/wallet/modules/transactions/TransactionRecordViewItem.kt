package bitcoin.wallet.modules.transactions

import bitcoin.wallet.entities.CoinValue
import java.util.*

data class TransactionRecordViewItem(
        val hash: String,
        val amount: CoinValue,
        val fee: CoinValue,
        val from: String,
        val to: String,
        val incoming: Boolean,
        val blockHeight: Long,
        val date: Date,
        val status: Status?,
        val confirmations: Long?,
        val valueInBaseCurrency: Double,
        val exchangeRate: Double? = null
) {

    enum class Status {
        SUCCESS, PENDING
    }

    override fun equals(other: Any?): Boolean {
        if (other is TransactionRecordViewItem) {
            return hash == other.hash
                    && amount == other.amount
                    && fee == other.fee
                    && from == other.from
                    && to == other.to
                    && incoming == other.incoming
                    && blockHeight == other.blockHeight
                    && date == other.date
                    && status == other.status
                    && confirmations == other.confirmations
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + fee.hashCode()
        result = 31 * result + from.hashCode()
        result = 31 * result + to.hashCode()
        result = 31 * result + incoming.hashCode()
        result = 31 * result + blockHeight.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + (status?.hashCode() ?: 0)
        result = 31 * result + (confirmations?.hashCode() ?: 0)
        return result
    }

}
