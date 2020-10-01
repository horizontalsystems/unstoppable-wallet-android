package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin

object SelectSwapCoinModule {

    const val excludedCoinKey = "excludedCoinKey"
    const val hideZeroBalanceKey = "hideZeroBalanceKey"
    const val selectedCoinKey = "selectedCoinKey"

    fun start(context: Activity, requestCode: Int, hideZeroBalance: Boolean, excludedCoin: Coin?) {
        val intent = Intent(context, SelectSwapCoinActivity::class.java)
        intent.putExtra(excludedCoinKey, excludedCoin)
        intent.putExtra(hideZeroBalanceKey, hideZeroBalance)

        context.startActivityForResult(intent, requestCode)
    }

    class Factory(private val excludedCoin: Coin?, private val hideZeroBalance: Boolean?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelectSwapCoinViewModel(excludedCoin, hideZeroBalance, App.coinManager, App.walletManager, App.adapterManager) as T
        }
    }

}
