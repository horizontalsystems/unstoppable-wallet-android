package cash.p.terminal.modules.transactions

import cash.p.terminal.wallet.Clearable
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.contacts.model.Contact
import io.horizontalsystems.core.entities.Blockchain
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
}
