package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import java.math.BigDecimal
import java.util.*

class TransactionViewItemFactory(private val feeCoinProvider: FeeCoinProvider) {

    fun item(wallet: Wallet, transactionItem: TransactionItem, lastBlockHeight: Int?, threshold: Int, rate: CurrencyValue?): TransactionViewItem {
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

        val sentToSelf = isSentToSelf(record)
        val lockInfo = record.lockInfo
        val incoming = record.amount > BigDecimal.ZERO
        var toAddress: String? = null
        var fromAddress: String? = null

        if (incoming) {
            fromAddress = record.from.firstOrNull { !it.mine }?.address
        } else {
            toAddress = record.to.firstOrNull { !it.mine }?.address
        }

        var amount = record.amount
        val currencyValue = rate?.let {
            val amountLocked = lockInfo?.amount
            if (sentToSelf && amountLocked != null) {
                amount = amountLocked
            }

            CurrencyValue(it.currency, amount * it.value)
        }

        val coin = transactionItem.wallet.coin
        val date = if (record.timestamp == 0L) null else Date(record.timestamp * 1000)

        val feeCoinValue = transactionItem.record.fee?.let {
            val feeCoin = feeCoinProvider.feeCoinData(coin)?.first ?: coin
            CoinValue(feeCoin, transactionItem.record.fee)
        }

        return TransactionViewItem(
                wallet,
                record.transactionHash,
                CoinValue(coin, amount),
                currencyValue,
                feeCoinValue,
                fromAddress,
                toAddress,
                sentToSelf,
                showFromAddress(wallet.coin.type),
                incoming,
                date,
                status,
                rate,
                lockInfo
        )
    }

    private fun isSentToSelf(record: TransactionRecord): Boolean {
        val allFromAddressesMine = record.from.all { it.mine }
        val allToAddressesMine = record.to.all { it.mine }

        return allFromAddressesMine && allToAddressesMine
    }

    private fun showFromAddress(coinType: CoinType): Boolean {
        return !(coinType == CoinType.Bitcoin || coinType == CoinType.BitcoinCash || coinType == CoinType.Dash)
    }

}
