package cash.p.terminal.modules.swapx.settings.uniswap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.swapx.settings.RecipientAddressViewModel
import cash.p.terminal.modules.swapx.settings.SwapDeadlineViewModel
import cash.p.terminal.modules.swapx.settings.SwapSlippageViewModel

object UniswapSettingsModule {

    sealed class State {
        class Valid(val tradeOptions: SwapTradeOptions) : State()
        object Invalid : State()
    }

    class Factory(
        private val recipient: Address?,
    ) : ViewModelProvider.Factory {

        private val service by lazy { UniswapSettingsService(recipient) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                UniswapSettingsViewModel::class.java -> UniswapSettingsViewModel(service) as T
                SwapDeadlineViewModel::class.java -> SwapDeadlineViewModel(service) as T
                SwapSlippageViewModel::class.java -> SwapSlippageViewModel(service) as T
                RecipientAddressViewModel::class.java -> RecipientAddressViewModel(service) as T
                else -> throw IllegalArgumentException()
            }
        }
    }
}