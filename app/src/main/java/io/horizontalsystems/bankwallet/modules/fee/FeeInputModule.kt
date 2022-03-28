package io.horizontalsystems.bankwallet.modules.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.Coin

object FeeInputModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val coin: Coin,
        private val coinDecimal: Int,
        private val fiatDecimal: Int
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FeeInputViewModel(
                coin,
                coinDecimal,
                fiatDecimal,
                App.currencyManager,
                App.marketKit
            ) as T
        }

    }

}