package io.horizontalsystems.bankwallet.modules.fee

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import java.math.BigDecimal

@Composable
fun HSFeeInput(
    coinCode: String,
    coinDecimal: Int,
    fee: BigDecimal?,
    amountInputType: AmountInputType,
    rate: CurrencyValue?,
    navController: NavController
) {
    CellUniversalLawrenceSection(
        listOf {
            HSFeeInputRaw(
                coinCode = coinCode,
                coinDecimal = coinDecimal,
                fee = fee,
                amountInputType = amountInputType,
                rate = rate,
                navController = navController
            )
        })
}

@Composable
fun HSFeeInputRaw(
    title: String = stringResource(R.string.Send_Fee),
    info: String = stringResource(R.string.Send_Fee_Info),
    coinCode: String,
    coinDecimal: Int,
    fee: BigDecimal?,
    amountInputType: AmountInputType,
    rate: CurrencyValue?,
    navController: NavController
) {

    var formatted by remember { mutableStateOf<FeeItem?>(null) }

    LaunchedEffect(fee, amountInputType, rate) {
        formatted = getFormatted(fee, rate, coinCode, coinDecimal, amountInputType)
    }

    FeeCell(
        title = title,
        info = info,
        value = formatted,
        viewState = null,
        navController = navController
    )
}

@Composable
fun HSFeeInputRawWithViewState(
    title: String = stringResource(R.string.Send_Fee),
    info: String = stringResource(R.string.Send_Fee_Info),
    coinCode: String,
    coinDecimal: Int,
    fee: BigDecimal?,
    viewState: ViewState,
    amountInputType: AmountInputType,
    rate: CurrencyValue?,
    navController: NavController
) {
    var formatted by remember { mutableStateOf<FeeItem?>(null) }

    LaunchedEffect(fee, amountInputType, rate) {
        formatted = getFormatted(fee, rate, coinCode, coinDecimal, amountInputType)
    }

    FeeCell(
        title = title,
        info = info,
        value = formatted,
        viewState = viewState,
        navController = navController
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