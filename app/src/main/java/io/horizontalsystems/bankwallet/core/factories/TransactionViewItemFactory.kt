package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import java.util.*

class TransactionViewItemFactory {

    fun item(
        wallet: Wallet,
        record: TransactionRecord,
        lastBlockInfo: LastBlockInfo?,
        mainAmountCurrencyValue: CurrencyValue? = null
    ): TransactionViewItem {
        return TransactionViewItem(
            wallet,
            record,
            record.getType(lastBlockInfo),
            Date(record.timestamp),
            record.status(lastBlockInfo?.height),
            mainAmountCurrencyValue
        )
    }

}
