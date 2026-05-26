package io.horizontalsystems.bankwallet.modules.availablebalance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import java.math.BigDecimal

@HiltViewModel(assistedFactory = AvailableBalanceViewModel.Factory::class)
class AvailableBalanceViewModel @AssistedInject constructor(
    @Assisted private val coinCode: String,
    @Assisted("coinDecimal") private val coinDecimal: Int,
    @Assisted("fiatDecimal") private val fiatDecimal: Int,
    private val numberFormatter: IAppNumberFormatter,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            coinCode: String,
            @Assisted("coinDecimal") coinDecimal: Int,
            @Assisted("fiatDecimal") fiatDecimal: Int,
        ): AvailableBalanceViewModel
    }

    var amountInputType: AmountInputType? = null
    var availableBalance: BigDecimal? = null
    var xRate: CurrencyValue? = null

    var formatted by mutableStateOf<String?>(null)
        private set

    fun refreshFormatted() {
        val tmpAvailableBalance = availableBalance
        val tmpAmountInputMode = amountInputType

        formatted = when {
            tmpAvailableBalance == null || tmpAmountInputMode == null -> null
            tmpAmountInputMode == AmountInputType.COIN -> {
                numberFormatter.formatCoinFull(tmpAvailableBalance, coinCode, coinDecimal)
            }
            tmpAmountInputMode == AmountInputType.CURRENCY -> {
                xRate
                    ?.let {
                        it.copy(value = tmpAvailableBalance.times(it.value))
                    }
                    ?.getFormattedFull()
            }
            else -> null
        }
    }
}

object AvailableBalanceModule
