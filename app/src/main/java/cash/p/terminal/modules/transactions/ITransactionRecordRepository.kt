package cash.p.terminal.modules.transactions

import cash.p.terminal.core.Clearable
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.Observable

interface ITransactionRecordRepository : Clearable {
    val itemsObservable: Observable<List<TransactionRecord>>

    fun setWallets(
        transactionWallets: List<TransactionWallet>,
        wallet: TransactionWallet?,
        transactionType: FilterTransactionType,
        blockchain: Blockchain?
    )
    fun setWalletAndBlockchain(transactionWallet: TransactionWallet?, blockchain: Blockchain?)
    fun setTransactionType(transactionType: FilterTransactionType)
    fun loadNext()
}
