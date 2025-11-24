package io.horizontalsystems.bankwallet.modules.send.zcash.shield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationScreen

@Composable
fun ShieldZcashScreen(
    navController: NavController,
    viewModel: ShieldZcashViewModel,
    sendEntryPointDestId: Int
) {
    var confirmationData by remember { mutableStateOf(viewModel.getConfirmationData()) }
    var refresh by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        if (refresh) {
            confirmationData = viewModel.getConfirmationData()
        }

        onPauseOrDispose {
            refresh = true
        }
    }

    SendConfirmationScreen(
        navController = navController,
        coinMaxAllowedDecimals = viewModel.coinMaxAllowedDecimals,
        feeCoinMaxAllowedDecimals = viewModel.coinMaxAllowedDecimals,
        amountInputType = AmountInputType.COIN,
        rate = viewModel.coinRate,
        feeCoinRate = viewModel.coinRate,
        sendResult = viewModel.sendResult,
        blockchainType = viewModel.blockchainType,
        coin = confirmationData.coin,
        feeCoin = confirmationData.feeCoin,
        amount = confirmationData.amount,
        address = null,
        contact = confirmationData.contact,
        fee = viewModel.fee,
        lockTimeInterval = confirmationData.lockTimeInterval,
        memo = confirmationData.memo,
        rbfEnabled = confirmationData.rbfEnabled,
        onClickSend = viewModel::onClickSend,
        sendEntryPointDestId = sendEntryPointDestId,
        title = stringResource(R.string.Balance_Zcash_UnshieldedBalance_Shield)
    )

}
