package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import java.util.*

class TransactionViewItemFactory(
        private val walletManager: IWalletManager,
        private val currencyManager: ICurrencyManager,
        private val rateManager: RateManager) {

    private val latestRateFallbackThreshold: Long = 60 // minutes

    fun item(record: TransactionRecord): TransactionViewItem {
        val adapter = walletManager.wallets.firstOrNull { it.coin == record.coin }?.adapter

        var rateValue: Double? = null

        if (record.rate == 0.0) {
            val secondsAgo = DateHelper.getSecondsAgo(record.timestamp * 1000)
            if (secondsAgo < latestRateFallbackThreshold * 60) {
                val rate = rateManager.latestRates[record.coin]?.get(currencyManager.baseCurrency.code)
                rate?.let {
                    rateValue = it.value
                }
            }
        } else {
            rateValue = record.rate
        }

        val convertedValue = rateValue?.let { it * record.amount } ?: run { null }

        var status: TransactionStatus = TransactionStatus.Pending

        val lastBlockHeight = adapter?.lastBlockHeight

        if (record.blockHeight != 0L && lastBlockHeight != null) {

            val confirmations = lastBlockHeight - record.blockHeight + 1

            val threshold = adapter.confirmationsThreshold

            status = when {
                confirmations >= threshold -> TransactionStatus.Completed
                else -> TransactionStatus.Processing((confirmations / threshold.toDouble()).times(100).toInt())
            }
        }

        val incoming = record.amount > 0

        return TransactionViewItem(
                record.transactionHash,
                CoinValue(record.coin, record.amount),
                convertedValue?.let { CurrencyValue(currencyManager.baseCurrency, it) },
                record.from.firstOrNull { it.mine != incoming }?.address,
                record.to.firstOrNull { it.mine == incoming }?.address,
                incoming,
                if (record.timestamp == 0L) null else Date(record.timestamp * 1000),
                status
        )
    }
}
