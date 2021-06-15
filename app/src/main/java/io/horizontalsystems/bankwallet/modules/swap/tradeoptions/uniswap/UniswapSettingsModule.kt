package io.horizontalsystems.bankwallet.modules.swap.tradeoptions.uniswap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.AddressResolutionService
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.SwapDeadlineViewModel
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.SwapSlippageViewModel
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapTradeService

object UniswapSettingsModule {

    sealed class State {
        class Valid(val tradeOptions: SwapTradeOptions) : State()
        object Invalid : State()
    }

    class Factory(
            private val tradeService: UniswapTradeService,
            private val dex: SwapMainModule.Dex
    ) : ViewModelProvider.Factory {

        private val service by lazy { UniswapSettingsService(tradeService.tradeOptions) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val evmCoin = dex.coin ?: throw IllegalArgumentException()

            return when (modelClass) {
                UniswapSettingsViewModel::class.java -> UniswapSettingsViewModel(service, tradeService) as T
                SwapDeadlineViewModel::class.java -> SwapDeadlineViewModel(service) as T
                SwapSlippageViewModel::class.java -> SwapSlippageViewModel(service) as T
                RecipientAddressViewModel::class.java -> {
                    val addressParser = App.addressParserFactory.parser(evmCoin)
                    val resolutionService = AddressResolutionService(evmCoin.code, true)
                    val placeholder = Translator.getString(R.string.SwapSettings_RecipientPlaceholder)
                    RecipientAddressViewModel(service, resolutionService, addressParser, placeholder, listOf(service, resolutionService)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
