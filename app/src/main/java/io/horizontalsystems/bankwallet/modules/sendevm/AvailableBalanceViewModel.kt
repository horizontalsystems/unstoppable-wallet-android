package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal

class AvailableBalanceViewModel(
    private val coinCode: String,
    private val coinDecimal: Int,
    private val fiatDecimal: Int
) : ViewModel() {

    var amountInputType: SendModule.InputType? = null
    var availableBalance: BigDecimal? = null
    var xRate: CurrencyValue? = null

    var formatted by mutableStateOf<String?>(null)
        private set

    fun refreshFormatted() {
        val tmpAvailableBalance = availableBalance
        val tmpAmountInputMode = amountInputType

        formatted = when {
            tmpAvailableBalance == null || tmpAmountInputMode == null -> null
            tmpAmountInputMode == SendModule.InputType.COIN -> {
                App.numberFormatter.formatCoin(tmpAvailableBalance, coinCode, 0, coinDecimal)
            }
            tmpAmountInputMode == SendModule.InputType.CURRENCY -> {
                xRate
                    ?.let {
                        it.copy(value = tmpAvailableBalance.times(it.value))
                    }
                    ?.getFormatted(fiatDecimal, fiatDecimal)
            }
            else -> null
        }
    }
}

object AvailableBalanceModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val coinCode: String,
        private val coinDecimal: Int,
        private val fiatDecimal: Int,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AvailableBalanceViewModel(
                coinCode,
                coinDecimal,
                fiatDecimal
            ) as T
        }
    }
}
