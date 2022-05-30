package io.horizontalsystems.bankwallet.modules.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object FeeInputModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val coinCode: String,
        private val coinDecimal: Int,
        private val fiatDecimal: Int
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FeeInputViewModel(
                coinCode,
                coinDecimal,
                fiatDecimal
            ) as T
        }

    }

}