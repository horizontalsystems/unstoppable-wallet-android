package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.util.*

data class TransactionRecordViewItem(
        val hash: String,
        val adapterId: String,
        val amount: CoinValue,
        val fee: CoinValue,
        val from: String?,
        val to: String?,
        val incoming: Boolean,
        val blockHeight: Long?,
        val date: Date?,
        val status: TransactionStatus,
        var currencyAmount: CurrencyValue? = null,
        var exchangeRate: Double? = null
) {

    override fun equals(other: Any?): Boolean {
        if (other is TransactionRecordViewItem) {
            return hash == other.hash
                    && adapterId == other.adapterId
                    && amount == other.amount
                    && fee == other.fee
                    && from == other.from
                    && to == other.to
                    && incoming == other.incoming
                    && date == other.date
                    && currencyAmount == other.currencyAmount
                    && exchangeRate == other.exchangeRate
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + adapterId.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + fee.hashCode()
        result = 31 * result + (from?.hashCode() ?: 0)
        result = 31 * result + (to?.hashCode() ?: 0)
        result = 31 * result + incoming.hashCode()
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + (currencyAmount?.hashCode() ?: 0)
        result = 31 * result + (exchangeRate?.hashCode() ?: 0)
        return result
    }

}
