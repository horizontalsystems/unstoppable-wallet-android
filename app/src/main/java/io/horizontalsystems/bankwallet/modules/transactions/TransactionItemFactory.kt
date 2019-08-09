package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet

class TransactionItemFactory {

    fun createTransactionItem(wallet: Wallet, record: TransactionRecord): TransactionItem {
        return TransactionItem(wallet, record)
    }

}
