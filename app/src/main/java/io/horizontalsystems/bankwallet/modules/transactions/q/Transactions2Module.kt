package io.horizontalsystems.bankwallet.modules.transactions.q

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.balance.BalanceActiveWalletRepository
import io.horizontalsystems.coinkit.models.Coin

object Transactions2Module {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return Transactions2ViewModel(
                Transactions2Service(
                    BalanceActiveWalletRepository(App.walletManager, App.accountSettingManager),
                    TransactionRecordRepository(App.adapterManager),
                    TransactionsXRateRepository(App.currencyManager, App.xRateManager)
                ),
                TransactionViewItem2Factory()
            ) as T
        }
    }

    data class Filter(val coin: Coin?)
}