package io.horizontalsystems.bankwallet.modules.swap

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import java.math.BigDecimal

object SwapModule {

    const val tokenInKey = "tokenInKey"

    fun start(context: Context, tokenIn: Coin) {
        val intent = Intent(context, SwapActivity::class.java)
        intent.putExtra(tokenInKey, tokenIn)

        context.startActivity(intent)
    }

    class Factory(private val tokenIn: Coin?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapViewModel(App.uniswapKitManager, App.walletManager, App.adapterManager, tokenIn) as T
        }
    }

    data class CoinWithBalance(val coin: Coin, val balance: BigDecimal)

    sealed class ValidationError : Throwable() {
        class InsufficientBalance : ValidationError()
    }

}
