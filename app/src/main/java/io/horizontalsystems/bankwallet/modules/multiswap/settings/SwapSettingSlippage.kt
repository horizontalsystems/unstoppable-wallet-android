package io.horizontalsystems.bankwallet.modules.multiswap.settings

import android.util.Range
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.modules.multiswap.settings.ui.SlippageAmount
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputStateWarning
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

data class SwapSettingSlippage(
    val settings: Map<String, Any?>,
    val defaultSlippage: BigDecimal
) : ISwapSetting {
    override val id = "slippage"

    val value = settings[id] as? BigDecimal

    fun valueOrDefault() = value ?: defaultSlippage

    @Composable
    override fun GetContent(
        navController: NavController,
        onError: (Throwable?) -> Unit,
        onValueChange: (Any?) -> Unit
    ) {
        val viewModel = viewModel<SwapSlippageViewModel>(initializer = {
            SwapSlippageViewModel(SwapSlippageService(value, defaultSlippage))
        })
        val error = viewModel.errorState?.error

        LaunchedEffect(error) {
            if (error is FormsInputStateWarning) {
                onError.invoke(null)
            } else {
                onError.invoke(error)
            }
        }

        SlippageAmount(
            viewModel.inputFieldPlaceholder,
            viewModel.initialValue,
            viewModel.inputButtons,
            error
        ) {
            viewModel.onChangeText(it)

            onValueChange.invoke(it.toBigDecimalOrNull())
        }
    }
}

class SwapSlippageService(
    override val initialSlippage: BigDecimal?,
    override val defaultSlippage: BigDecimal
) : ISwapSlippageService {

    private val limitSlippageBounds = Range(BigDecimal("0.01"), BigDecimal("50"))
    private val usualHighestSlippage = BigDecimal(5)
    private var slippage: BigDecimal = initialSlippage ?: defaultSlippage

    override val slippageChangeObservable = PublishSubject.create<Unit>()
    override val recommendedSlippages = listOf(BigDecimal("0.1"), BigDecimal("1"))
    override var slippageError: Throwable? = null
    override val unusualSlippage: Boolean
        get() = slippage > usualHighestSlippage

    override fun setSlippage(value: BigDecimal) {
        slippage = value

        slippageError = when {
            slippage.compareTo(BigDecimal.ZERO) == 0 -> {
                SwapSettingsModule.SwapSettingsError.ZeroSlippage
            }
            slippage > limitSlippageBounds.upper -> {
                SwapSettingsModule.SwapSettingsError.InvalidSlippage(
                    SwapSettingsModule.InvalidSlippageType.Higher(limitSlippageBounds.upper)
                )
            }
            slippage < limitSlippageBounds.lower -> {
                SwapSettingsModule.SwapSettingsError.InvalidSlippage(
                    SwapSettingsModule.InvalidSlippageType.Lower(limitSlippageBounds.lower)
                )
            }
            else -> {
                null
            }
        }

        slippageChangeObservable.onNext(Unit)
    }
}
