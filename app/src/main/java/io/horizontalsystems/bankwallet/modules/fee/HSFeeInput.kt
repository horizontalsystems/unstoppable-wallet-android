package io.horizontalsystems.bankwallet.modules.fee

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCell
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.marketkit.models.Coin
import java.math.BigDecimal

@Composable
fun HSFeeInput(
    coin: Coin,
    coinDecimal: Int,
    fiatDecimal: Int,
    fee: BigDecimal,
    amountInputType: SendModule.InputType,
    onClick: (() -> Unit)? = null
) {
    val viewModel = viewModel<FeeInputViewModel>(
        factory = FeeInputModule.Factory(
            coin,
            coinDecimal,
            fiatDecimal
        )
    )
    val formatted = viewModel.formatted

    LaunchedEffect(fee, amountInputType) {
        viewModel.fee = fee
        viewModel.amountInputType = amountInputType
        viewModel.refreshFormatted()
    }

    EvmFeeCell(
        title = stringResource(R.string.Send_Fee),
        value = formatted ?: "",
        loading = false,
        viewState = null,
        onClick = onClick
    )
}
