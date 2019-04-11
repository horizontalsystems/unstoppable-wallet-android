package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import java.math.BigDecimal
import java.util.*

class TransactionViewItemFactory {

    fun item(transactionItem: TransactionItem, lastBlockHeight: Int?, threshold: Int?, rate: CurrencyValue?): TransactionViewItem {
        val record = transactionItem.record

        var status: TransactionStatus = TransactionStatus.Pending

        if (record.blockHeight != 0L && lastBlockHeight != null) {

            val confirmations = lastBlockHeight - record.blockHeight + 1

            if (confirmations >= 0) {
                status = when {
                    confirmations >= threshold ?: 1 -> TransactionStatus.Completed
                    else -> TransactionStatus.Processing(confirmations.toInt())
                }
            }
        }

        val incoming = record.amount > BigDecimal.ZERO

        var toAddress = if (incoming) null else record.to.firstOrNull { !it.mine }?.address
        var fromAddress = if (!incoming) null else record.from.firstOrNull { !it.mine }?.address

        if (toAddress == null && fromAddress == null) {
            toAddress = record.to.firstOrNull()?.address
            fromAddress = record.from.firstOrNull()?.address
        }

        val currencyValue = rate?.let { CurrencyValue(it.currency, record.amount * it.value) }

        return TransactionViewItem(
                record.transactionHash,
                transactionItem.coin,
                CoinValue(transactionItem.coin.code, record.amount),
                currencyValue,
                fromAddress,
                toAddress,
                incoming,
                if (record.timestamp == 0L) null else Date(record.timestamp * 1000),
                status,
                rate
        )
    }
}
