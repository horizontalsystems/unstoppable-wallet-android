package io.horizontalsystems.bankwallet.modules.fee

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import java.math.BigDecimal

@Composable
fun HSFeeInput(
    coinCode: String,
    coinDecimal: Int,
    fiatDecimal: Int,
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
                fiatDecimal = fiatDecimal,
                fee = fee,
                amountInputType = amountInputType,
                rate = rate,
                navController = navController
            )
        })
}
@Composable
fun HSFeeInputRaw(
    coinCode: String,
    coinDecimal: Int,
    fiatDecimal: Int,
    fee: BigDecimal?,
    amountInputType: AmountInputType,
    rate: CurrencyValue?,
    navController: NavController
) {
    val viewModel = viewModel<FeeInputViewModel>(
        factory = FeeInputModule.Factory(
            coinCode,
            coinDecimal,
            fiatDecimal
        )
    )
    val formatted = viewModel.formatted

    LaunchedEffect(fee, amountInputType, rate) {
        viewModel.fee = fee
        viewModel.amountInputType = amountInputType
        viewModel.rate = rate
        viewModel.refreshFormatted()
    }

    FeeCell(
        title = stringResource(R.string.Send_Fee),
        info = stringResource(R.string.Send_Fee_Info),
        value = formatted,
        viewState = null,
        navController = navController
    )
}
