package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.reactivex.Observable

interface ITransactionRecordRepository : Clearable {
    val itemsObservable: Observable<Pair<List<TransactionRecord>, Int>>

    fun setWallets(
        transactionWallets: List<TransactionWallet>,
        wallet: TransactionWallet?,
        transactionType: FilterTransactionType
    )
    fun setSelectedWallet(transactionWallet: TransactionWallet?)
    fun setTransactionType(transactionType: FilterTransactionType)
    fun loadNext()
}
