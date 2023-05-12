package io.horizontalsystems.bankwallet.modules.swap.settings.oneinch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSlippageViewModel
import java.math.BigDecimal

object OneInchSwapSettingsModule {

    val defaultSlippage = BigDecimal("1")

    data class OneInchSwapSettings(
        var slippage: BigDecimal = defaultSlippage,
        var gasPrice: Long? = null,
        var recipient: Address? = null
    )

    sealed class State {
        class Valid(val swapSettings: OneInchSwapSettings) : State()
        object Invalid : State()
    }

    class Factory(
        private val address: Address?,
        private val slippage: BigDecimal?,
    ) : ViewModelProvider.Factory {

        private val service by lazy { OneInchSettingsService(address, slippage) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                OneInchSettingsViewModel::class.java -> OneInchSettingsViewModel(service) as T
                SwapSlippageViewModel::class.java -> SwapSlippageViewModel(service) as T
                RecipientAddressViewModel::class.java -> RecipientAddressViewModel(service) as T
                else -> throw IllegalArgumentException()
            }
        }
    }
}
