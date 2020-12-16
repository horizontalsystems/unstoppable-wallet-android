package io.horizontalsystems.bankwallet.modules.market.top

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object MarketTopModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MarketTopViewModel() as T
        }

    }

}
