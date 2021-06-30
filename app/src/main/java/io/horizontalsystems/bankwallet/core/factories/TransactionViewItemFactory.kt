package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import java.util.*

class TransactionViewItemFactory {

    fun item(wallet: Wallet, record: TransactionRecord, lastBlockInfo: LastBlockInfo?, rate: CurrencyValue?): TransactionViewItem {
        val currencyValue = getCurrencyValue(record, rate)

        return TransactionViewItem(
                wallet,
                record,
                record.getType(lastBlockInfo),
                Date(record.timestamp),
                record.status(lastBlockInfo?.height),
                currencyValue
        )
    }

    private fun getCurrencyValue(record: TransactionRecord, rate: CurrencyValue?): CurrencyValue? {
        rate ?: return null
        val amount = record.mainAmount ?: return null
        return CurrencyValue(rate.currency, amount * rate.value)
    }
}
