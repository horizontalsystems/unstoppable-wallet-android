package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord

class TransactionItemFactory {

    fun createTransactionItem(coinCode: CoinCode, record: TransactionRecord): TransactionItem {
        return TransactionItem(coinCode, record)
    }

}
