package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.reactivex.Observable

interface ITransactionRecordRepository {
    val itemsObservable: Observable<List<TransactionRecord>>

    fun setWallets(wallets: List<Wallet>)
    fun setSelectedWallet(wallet: Wallet?)
    fun loadNext()
}
