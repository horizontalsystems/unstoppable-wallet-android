package io.horizontalsystems.walletkit.modules.transactions

import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.flow.Flow

interface ITransactionRecordRepository : Clearable {
    val itemsFlow: Flow<List<TransactionRecord>>

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
