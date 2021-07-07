package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import java.util.*

object TransactionInfoModule {

    class Factory(private val transactionViewItem: TransactionViewItem) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val adapter = App.adapterManager.getTransactionsAdapterForWallet(transactionViewItem.wallet)!!
            val service = TransactionInfoService(
                TextHelper,
                adapter,
                App.xRateManager,
                App.currencyManager,
                App.feeCoinProvider,
                App.buildConfigProvider,
                App.accountSettingManager
            )
            return TransactionInfoViewModel(service, transactionViewItem.record, transactionViewItem.wallet, TransactionInfoAddressMapper) as T
        }

    }

    data class TitleViewItem(
        val date: Date?,
        val primaryAmountInfo: SendModule.AmountInfo,
        val secondaryAmountInfo: SendModule.AmountInfo?,
        val type: TransactionType,
        val lockState: TransactionLockState?
    )

    data class ExplorerData(val title: String, val url: String?)
}
