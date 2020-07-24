package io.horizontalsystems.bankwallet.modules.swap

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.entities.Coin

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
            return SwapViewModel(tokenIn) as T
        }
    }

}
