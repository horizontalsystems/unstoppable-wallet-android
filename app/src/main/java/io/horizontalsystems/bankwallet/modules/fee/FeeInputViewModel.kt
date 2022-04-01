package io.horizontalsystems.bankwallet.modules.fee

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal

class FeeInputViewModel(
    private val coinCode: String,
    private val coinDecimal: Int,
    private val fiatDecimal: Int
) : ViewModel() {

    var amountInputType: SendModule.InputType? = null
    var fee = BigDecimal.ZERO
    var rate: CurrencyValue? = null

    var formatted by mutableStateOf<String?>(null)
        private set

    fun refreshFormatted() {
        val tmpAmountInputType = amountInputType ?: return

        val values = mutableListOf(
            App.numberFormatter.formatCoin(fee, coinCode, 0, coinDecimal)
        )

        rate?.let {
            val currencyStr = it.copy(value = fee.times(it.value)).getFormatted(fiatDecimal, fiatDecimal)

            when (tmpAmountInputType) {
                SendModule.InputType.COIN -> values.add(currencyStr)
                SendModule.InputType.CURRENCY -> values.add(0, currencyStr)
            }
        }

        formatted = values.joinToString(" | ")
    }

}