package io.horizontalsystems.bankwallet.modules.transactions.q

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoAddressMapper
import io.horizontalsystems.bankwallet.modules.transactions.*
import io.horizontalsystems.coinkit.models.Coin

object Transactions2Module {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val dataSource = TransactionRecordDataSource(
                PoolRepo(), TransactionItemDataSource(), 20, TransactionViewItemFactory(
                TransactionInfoAddressMapper, App.numberFormatter
            ), TransactionMetadataDataSource()
            )

            return Transactions2ViewModel(
                Transactions2Service(TransactionRecordRepository(dataSource)),
                TransactionViewItem2Factory()
            ) as T
        }
    }

    data class Filter(val coin: Coin?)
}