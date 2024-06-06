package io.horizontalsystems.bankwallet.modules.send.evm

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRightForResult
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.SendScreenCommon
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationFragment
import io.horizontalsystems.bankwallet.modules.sendtokenselect.PrefilledData
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun SendEvmScreen(
    title: String,
    navController: NavController,
    amountInputModeViewModel: AmountInputModeViewModel,
    prefilledData: PrefilledData?,
    wallet: Wallet,
    predefinedAddress: String?,
) {
    val viewModel = viewModel<SendEvmViewModel>(factory = SendEvmModule.Factory(wallet, predefinedAddress))
    val uiState = viewModel.uiState

    val view = LocalView.current

    SendScreenCommon(
        navController = navController,
        wallet = wallet,
        prefilledData = prefilledData,
        title = title,
        coinDecimals = viewModel.coinMaxAllowedDecimals,
        onEnterAmount = viewModel::onEnterAmount,
        onEnterAmountPercentage = viewModel::onEnterAmountPercentage,
        onEnterFiatAmount = viewModel::onEnterFiatAmount,
        onEnterAddress = viewModel::onEnterAddress,
        onProceed = {
            if (viewModel.hasConnection()) {
                viewModel.getSendData()?.let {
                    navController.slideFromRightForResult<SendEvmConfirmationFragment.Result>(
                        R.id.sendEvmConfirmationFragment,
                        SendEvmConfirmationFragment.Input(
                            sendData = it,
                            blockchainType = viewModel.wallet.token.blockchainType
                        )
                    ) {
                        if (it.success) {
                            navController.popBackStack()
                        }
                    }
                }
            } else {
                HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
            }
        },
        uiState = uiState
    )
}
