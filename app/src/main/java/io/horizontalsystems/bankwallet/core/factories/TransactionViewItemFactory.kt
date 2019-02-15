package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import java.math.BigDecimal
import java.util.*

class TransactionViewItemFactory(
        private val adapterManager: IAdapterManager,
        private val currencyManager: ICurrencyManager,
        private val rateManager: RateManager) {

    private val latestRateFallbackThreshold: Long = 60 // minutes

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

        val toAddress = when (incoming) {
            true -> record.to.find { it.mine }?.address
            false -> record.to.find { !it.mine }?.address ?: record.to.find { it.mine }?.address
        }

        val currencyValue = rate?.let { CurrencyValue(it.currency, record.amount * it.value) }

        return TransactionViewItem(
                record.transactionHash,
                CoinValue(transactionItem.coinCode, record.amount),
                currencyValue,
                record.from.firstOrNull { it.mine != incoming }?.address,
                toAddress,
                incoming,
                if (record.timestamp == 0L) null else Date(record.timestamp * 1000),
                status
        )
    }
}
