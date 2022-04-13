package io.horizontalsystems.bankwallet.modules.amount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.send.SendModule

object AmountInputModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val coinCode: String,
        private val coinDecimal: Int,
        private val fiatDecimal: Int,
        private val inputType: SendModule.InputType
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AmountInputViewModel2(
                coinCode,
                coinDecimal,
                fiatDecimal,
                inputType
            ) as T
        }

    }
}
