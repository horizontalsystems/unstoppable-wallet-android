package io.horizontalsystems.bankwallet.modules.fee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import java.math.BigDecimal

@Composable
fun HSFee(
    coinCode: String,
    coinDecimal: Int,
    fee: BigDecimal?,
    amountInputType: AmountInputType,
    rate: CurrencyValue?,
    navController: NavController,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(vertical = 8.dp)
    ) {
        HSFeeRaw(
            coinCode = coinCode,
            coinDecimal = coinDecimal,
            fee = fee,
            amountInputType = amountInputType,
            rate = rate,
            navController = navController,
        )
    }
}

@Composable
fun HSFeeRaw(
    title: String = stringResource(R.string.Send_Fee),
    info: String = stringResource(R.string.Send_Fee_Info),
    coinCode: String,
    coinDecimal: Int,
    fee: BigDecimal?,
    amountInputType: AmountInputType,
    rate: CurrencyValue?,
    navController: NavController,
) {

    var formatted by remember { mutableStateOf<FeeItem?>(null) }

    LaunchedEffect(fee, amountInputType, rate) {
        formatted = getFormatted(fee, rate, coinCode, coinDecimal, amountInputType)
    }

    DataFieldFeeTemplate(
        navController = navController,
        primary = formatted?.primary ?: "---",
        secondary = formatted?.secondary ?: "---",
        title = title,
        infoText = info
    )
}

private fun getFormatted(
    fee: BigDecimal?,
    rate: CurrencyValue?,
    coinCode: String,
    coinDecimal: Int,
    amountInputType: AmountInputType
): FeeItem? {

    if (fee == null) return null

    val coinAmount = App.numberFormatter.formatCoinFull(fee, coinCode, coinDecimal)
    val currencyAmount = rate?.let {
        it.copy(value = fee.times(it.value)).getFormattedFull()
    }

    return if (amountInputType == AmountInputType.CURRENCY && currencyAmount != null) {
        FeeItem(currencyAmount, coinAmount)
    } else {
        FeeItem(coinAmount, currencyAmount)
    }
}