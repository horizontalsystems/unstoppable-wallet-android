package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import java.util.*

class TransactionViewItemFactory(private val feeCoinProvider: FeeCoinProvider) {

    fun item(wallet: Wallet, record: TransactionRecord, lastBlockInfo: LastBlockInfo?, threshold: Int, rate: CurrencyValue?): TransactionViewItem {
        val currencyValue = rate?.let {
            CurrencyValue(it.currency, record.amount * it.value)
        }

        val date = if (record.timestamp == 0L) null else Date(record.timestamp * 1000)

        return TransactionViewItem(
                wallet,
                record,
                CoinValue(wallet.coin, record.amount),
                currencyValue,
                record.type,
                date,
                record.status(lastBlockInfo?.height, threshold),
                record.lockState(lastBlockInfo?.timestamp),
                record.conflictingTxHash != null
        )
    }
}
