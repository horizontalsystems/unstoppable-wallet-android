package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord

class TransactionItemFactory {

    fun createTransactionItem(coin: Coin, record: TransactionRecord): TransactionItem {
        return TransactionItem(coin, record)
    }

}
