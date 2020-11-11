package io.horizontalsystems.bankwallet.modules.swap.settings

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.Observable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

object SwapSettingsModule {
    @Parcelize
    data class SwapSettings(
            val slippage: BigDecimal,
            val deadline: Long,
            val recipientAddress: String? = null
    ) : Parcelable {
        fun toTradeOptions(): TradeOptions {
            return TradeOptions(slippage, deadline * 60, recipientAddress?.let { Address(it) })
        }
    }

    interface ISwapSettingsService {
        val stateObservable: Observable<SwapSettingsState>

        val slippage: BigDecimal?
        val defaultSlippage: BigDecimal
        val recommendedSlippageRange: ClosedRange<BigDecimal>

        val deadline: Long?
        val defaultDeadline: Long
        val recommendedDeadlineRange: ClosedRange<Long>

        val recipientAddress: String?

        fun enterSlippage(slippage: BigDecimal?)
        fun enterDeadline(deadline: Long?)
        fun enterRecipientAddress(address: String?)
    }

    sealed class SwapSettingsState {
        class Valid(val settings: SwapSettings) : SwapSettingsState()
        class Invalid(val errors: List<SwapSettingsError>) : SwapSettingsState()
    }

    sealed class SwapSettingsError {
        sealed class SlippageError : SwapSettingsError() {
            class SlippageTooLow(val minValue: BigDecimal) : SlippageError()
            class SlippageTooHigh(val maxValue: BigDecimal) : SlippageError()
        }
        sealed class DeadlineError : SwapSettingsError() {
            class DeadlineTooLow(val minValue: Long) : DeadlineError()
        }
        sealed class AddressError : SwapSettingsError() {
            object InvalidAddress : AddressError()
        }
    }

    class Factory(
            private val currentSwapSettings: SwapSettings,
            private val defaultSwapSettings: SwapSettings
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = SwapSettingsService(currentSwapSettings, defaultSwapSettings)
            val stringProvider = StringProvider(App.instance)

            return SwapSettingsViewModel(service, stringProvider) as T
        }
    }
}
