package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.managers.UnstoppableDomainsService
import io.horizontalsystems.bankwallet.modules.swap.SwapTradeService

object SwapTradeOptionsModule {

    class Factory(private val tradeService: SwapTradeService) : ViewModelProvider.Factory {

        private val service by lazy { SwapTradeOptionsService(tradeService, UnstoppableDomainsService()) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SwapTradeOptionsViewModel::class.java -> SwapTradeOptionsViewModel(service) as T
                SwapDeadlineViewModel::class.java -> SwapDeadlineViewModel(service) as T
                SwapSlippageViewModel::class.java -> SwapSlippageViewModel(service) as T
                RecipientAddressViewModel::class.java -> RecipientAddressViewModel(service) as T
                else -> throw IllegalArgumentException()
            }
        }
    }
}
