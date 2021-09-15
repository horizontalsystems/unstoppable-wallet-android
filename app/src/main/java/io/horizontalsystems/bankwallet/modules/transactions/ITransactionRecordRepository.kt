package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.reactivex.Observable

interface ITransactionRecordRepository : Clearable {
    val typesObservable: Observable<Pair<List<FilterTransactionType>, FilterTransactionType>>
    val walletsObservable: Observable<Pair<List<TransactionWallet>, TransactionWallet?>>
    val itemsObservable: Observable<List<TransactionRecord>>

    fun setWallets(transactionWallets: List<TransactionWallet>, walletsGroupedBySource: List<TransactionWallet>)
    fun setSelectedWallet(transactionWallet: TransactionWallet?)
    fun setTransactionType(transactionType: FilterTransactionType)
    fun loadNext()
}
