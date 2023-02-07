package cash.p.terminal.modules.fee

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.App
import cash.p.terminal.entities.CurrencyValue
import cash.p.terminal.modules.amount.AmountInputType
import java.math.BigDecimal

class FeeInputViewModel(
    private val coinCode: String,
    private val coinDecimal: Int,
    private val fiatDecimal: Int
) : ViewModel() {

    var amountInputType: AmountInputType? = null
    var fee: BigDecimal? = null
    var rate: CurrencyValue? = null

    var formatted by mutableStateOf<String?>(null)
        private set

    fun refreshFormatted() {
        val tmpAmountInputType = amountInputType ?: return
        val tmpFee = fee

        if (tmpFee != null) {
            val values = mutableListOf(
                App.numberFormatter.formatCoinFull(tmpFee, coinCode, coinDecimal)
            )

            rate?.let {
                val currencyStr = it.copy(value = tmpFee.times(it.value)).getFormattedFull()

                when (tmpAmountInputType) {
                    AmountInputType.COIN -> values.add(currencyStr)
                    AmountInputType.CURRENCY -> values.add(0, currencyStr)
                }
            }

            formatted = values.joinToString(" | ")
        } else {
            formatted = null
        }
    }

}