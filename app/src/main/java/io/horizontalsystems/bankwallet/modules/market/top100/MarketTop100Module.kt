package io.horizontalsystems.bankwallet.modules.market.top100

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object MarketTop100Module {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MarketTop100ViewModel() as T
        }

    }

}
