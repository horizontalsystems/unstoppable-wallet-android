package cash.p.terminal.modules.transactions

import cash.p.terminal.core.Clearable
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.Observable

interface ITransactionRecordRepository : Clearable {
    val itemsObservable: Observable<List<TransactionRecord>>

    fun set(
        transactionWallets: List<TransactionWallet>,
        wallet: TransactionWallet?,
        transactionType: FilterTransactionType,
        blockchain: Blockchain?
    )
    fun loadNext()
    fun reload()
}
