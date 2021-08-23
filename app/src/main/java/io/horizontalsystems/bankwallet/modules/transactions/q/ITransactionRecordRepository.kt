package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionWallet
import io.reactivex.Observable

interface ITransactionRecordRepository : Clearable {
    val itemsObservable: Observable<List<TransactionRecord>>

    fun setWallets(transactionWallets: List<TransactionWallet>, walletsGroupedBySource: List<TransactionWallet>)
    fun setSelectedWallet(transactionWallet: TransactionWallet?)
    fun loadNext()
}
