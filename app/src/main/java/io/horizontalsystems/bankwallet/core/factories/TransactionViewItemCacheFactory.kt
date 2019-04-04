package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItemCache
import java.math.BigDecimal
import java.util.*

class TransactionViewItemCacheFactory {

    fun item(transactionItem: TransactionItem, lastBlockHeight: Int?, threshold: Int?, rate: CurrencyValue?): TransactionViewItemCache {
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

        val currencyValue = rate?.let { CurrencyValue(it.currency, record.amount * it.value) }

        return TransactionViewItemCache(
                record.transactionHash,
                CoinValue(transactionItem.coin.code, record.amount),
                currencyValue,
                incoming,
                if (record.timestamp == 0L) null else Date(record.timestamp * 1000),
                status
        )
    }
}
