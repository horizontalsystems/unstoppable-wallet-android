package io.horizontalsystems.bankwallet.modules.swap_new.tradeoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.swap_new.SwapTradeService

object SwapTradeOptionsModule {

    class Factory(private val tradeService: SwapTradeService) : ViewModelProvider.Factory {

        private val service by lazy { SwapTradeOptionsService(tradeService.tradeOptions) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SwapTradeOptionsViewModel::class.java -> SwapTradeOptionsViewModel(service, tradeService) as T
                SwapDeadlineViewModel::class.java -> SwapDeadlineViewModel(service) as T
                RecipientAddressViewModel::class.java -> RecipientAddressViewModel(service) as T
                SwapSlippageViewModel::class.java -> SwapSlippageViewModel(service) as T
                else -> throw IllegalArgumentException()
            }
        }

    }
}
