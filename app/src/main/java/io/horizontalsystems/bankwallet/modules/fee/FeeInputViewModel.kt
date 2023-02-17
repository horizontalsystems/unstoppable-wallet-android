package io.horizontalsystems.bankwallet.modules.fee

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeViewItem
import java.math.BigDecimal

class FeeInputViewModel(
    private val coinCode: String,
    private val coinDecimal: Int,
    private val fiatDecimal: Int
) : ViewModel() {

    var amountInputType: AmountInputType? = null
    var fee: BigDecimal? = null
    var rate: CurrencyValue? = null

    var formatted by mutableStateOf<EvmFeeViewItem?>(null)
        private set

    fun refreshFormatted() {
        val tmpFee = fee

        formatted = if (tmpFee != null) {
            val coinAmount = App.numberFormatter.formatCoinFull(tmpFee, coinCode, coinDecimal)
            val currencyAmount = rate?.let {
                it.copy(value = tmpFee.times(it.value)).getFormattedFull()
            }
            EvmFeeViewItem(coinAmount, currencyAmount)
        } else {
            null
        }
    }

}