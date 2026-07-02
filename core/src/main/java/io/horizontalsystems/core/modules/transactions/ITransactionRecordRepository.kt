package io.horizontalsystems.core.modules.transactions

import io.horizontalsystems.core.core.Clearable
import io.horizontalsystems.core.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.core.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.Observable

interface ITransactionRecordRepository : Clearable {
    val itemsObservable: Observable<List<TransactionRecord>>

    fun set(
        transactionWallets: List<TransactionWallet>,
        wallet: TransactionWallet?,
        transactionType: FilterTransactionType,
        blockchain: Blockchain?,
        contact: Contact?
    )
    fun loadNext()
    fun reload()
    fun invalidateAdapters()
}
