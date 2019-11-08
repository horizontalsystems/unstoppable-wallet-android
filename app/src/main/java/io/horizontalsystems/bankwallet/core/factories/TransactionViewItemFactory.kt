package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.transactions.TransactionLockInfo
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
            CoinValue(feeCoin, transactionItem.record.fee)
        }

        val lockInfo = TransactionLockInfo.from(record.to.firstOrNull()?.pluginData)

        return TransactionViewItem(
                wallet,
                record.transactionHash,
                CoinValue(coin, record.amount),
                currencyValue,
                feeCoinValue,
                fromAddress,
                toAddress,
                isSentToSelf(record),
                showFromAddress(wallet.coin.type),
                incoming,
                if (record.timestamp == 0L) null else Date(record.timestamp * 1000),
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
