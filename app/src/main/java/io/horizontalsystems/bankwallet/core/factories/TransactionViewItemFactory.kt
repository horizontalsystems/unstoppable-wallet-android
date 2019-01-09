package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import java.util.*

class TransactionViewItemFactory(
        private val walletManager: IWalletManager,
        private val currencyManager: ICurrencyManager,
        private val rateManager: RateManager) {

    private val latestRateFallbackThreshold: Long = 60 // minutes

    fun item(record: TransactionRecord): TransactionViewItem {
        val adapter = walletManager.wallets.firstOrNull { it.coinCode == record.coinCode }?.adapter

        val rateValue = when {
            record.rate != 0.0 -> record.rate
            else -> null
        }

        val convertedValue = rateValue?.let { it * record.amount }

        var status: TransactionStatus = TransactionStatus.Pending

        val lastBlockHeight = adapter?.lastBlockHeight

        if (record.blockHeight != 0L && lastBlockHeight != null) {

            val confirmations = lastBlockHeight - record.blockHeight + 1

            if (confirmations >= 0) {

                val threshold = adapter.confirmationsThreshold

                status = when {
                    confirmations >= threshold -> TransactionStatus.Completed
                    else -> TransactionStatus.Processing(confirmations.toInt())
                }
            }
        }

        val incoming = record.amount > 0

        val toAddress = when(incoming) {
            true -> record.to.find { it.mine }?.address
            false -> record.to.find { !it.mine }?.address ?: record.to.find { it.mine }?.address
        }

        return TransactionViewItem(
                record.transactionHash,
                CoinValue(record.coinCode, record.amount),
                convertedValue?.let { CurrencyValue(currencyManager.baseCurrency, it) },
                record.from.firstOrNull { it.mine != incoming }?.address,
                toAddress,
                incoming,
                if (record.timestamp == 0L) null else Date(record.timestamp * 1000),
                status
        )
    }
}
