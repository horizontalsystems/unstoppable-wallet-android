package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import java.math.BigDecimal
import java.util.*

class TransactionViewItemFactory(private val feeCoinProvider: FeeCoinProvider) {

    fun item(transactionItem: TransactionItem, lastBlockHeight: Int?, threshold: Int, rate: CurrencyValue?): TransactionViewItem {
        val record = transactionItem.record

        var status: TransactionStatus = TransactionStatus.Pending

        if (record.blockHeight != null && lastBlockHeight != null) {

            val confirmations = lastBlockHeight - record.blockHeight.toInt() + 1
            if (confirmations >= 0) {
                status = when {
                    confirmations >= threshold -> TransactionStatus.Completed
                    else -> TransactionStatus.Processing(confirmations.toDouble() / threshold.toDouble())
                }
            }
        }

        val incoming = record.amount > BigDecimal.ZERO
        var toAddress: String? = null
        var fromAddress: String? = null

        if (incoming) {
            fromAddress = record.from.firstOrNull { !it.mine }?.address
        } else {
            toAddress = record.to.firstOrNull { !it.mine }?.address
        }

        val currencyValue = rate?.let { CurrencyValue(it.currency, record.amount * it.value) }
        val coin = transactionItem.wallet.coin

        val feeCoinValue = transactionItem.record.fee?.let {
            val feeCoin = feeCoinProvider.feeCoinData(coin)?.first ?: coin
            CoinValue(feeCoin.code, transactionItem.record.fee)
        }

        return TransactionViewItem(
                record.transactionHash,
                coin,
                CoinValue(coin.code, record.amount),
                currencyValue,
                feeCoinValue,
                fromAddress,
                toAddress,
                isSentToSelf(record),
                incoming,
                if (record.timestamp == 0L) null else Date(record.timestamp * 1000),
                status,
                rate
        )
    }

    private fun isSentToSelf(record: TransactionRecord): Boolean {
        val allFromAddressesMine = record.from.all { it.mine }
        val allToAddressesMine = record.to.all { it.mine }

        return allFromAddressesMine && allToAddressesMine
    }

}
