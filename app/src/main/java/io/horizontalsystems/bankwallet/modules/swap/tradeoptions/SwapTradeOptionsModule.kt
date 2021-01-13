package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap.SwapTradeService

object SwapTradeOptionsModule {

    class Factory(private val tradeService: SwapTradeService) : ViewModelProvider.Factory {

        private val service by lazy { SwapTradeOptionsService(tradeService.tradeOptions) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val ethereumCoin = App.appConfigProvider.ethereumCoin

            return when (modelClass) {
                SwapTradeOptionsViewModel::class.java -> SwapTradeOptionsViewModel(service, tradeService) as T
                SwapDeadlineViewModel::class.java -> SwapDeadlineViewModel(service) as T
                SwapSlippageViewModel::class.java -> SwapSlippageViewModel(service) as T
                RecipientAddressViewModel::class.java -> {
                    val addressParser = App.addressParserFactory.parser(ethereumCoin)
                    val resolutionService = AddressResolutionService(ethereumCoin.code, true)
                    val placeholder = App.instance.getString(R.string.SwapSettings_RecipientPlaceholder)
                    RecipientAddressViewModel(service, resolutionService, addressParser, placeholder) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
