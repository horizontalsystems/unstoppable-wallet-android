package cash.p.terminal.modules.swap.settings.uniswap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.modules.swap.settings.RecipientAddressViewModel
import cash.p.terminal.modules.swap.settings.SwapDeadlineViewModel
import cash.p.terminal.modules.swap.settings.SwapSlippageViewModel
import cash.p.terminal.modules.swap.uniswap.UniswapTradeService

object UniswapSettingsModule {

    sealed class State {
        class Valid(val tradeOptions: SwapTradeOptions) : State()
        object Invalid : State()
    }

    class Factory(
            private val tradeService: UniswapTradeService,
    ) : ViewModelProvider.Factory {

        private val service by lazy { UniswapSettingsService(tradeService.tradeOptions) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                UniswapSettingsViewModel::class.java -> UniswapSettingsViewModel(service, tradeService) as T
                SwapDeadlineViewModel::class.java -> SwapDeadlineViewModel(service) as T
                SwapSlippageViewModel::class.java -> SwapSlippageViewModel(service) as T
                RecipientAddressViewModel::class.java -> RecipientAddressViewModel(service) as T
                else -> throw IllegalArgumentException()
            }
        }
    }
}
