package io.horizontalsystems.bankwallet.modules.send.zcash.shield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

@Composable
fun ShieldZcashScreen(
    backStack: NavBackStack<HSScreen>,
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

//    TODO("xxx nav3")
//    SendConfirmationScreen(
//        navController = backStack,
//        coinMaxAllowedDecimals = viewModel.coinMaxAllowedDecimals,
//        feeCoinMaxAllowedDecimals = viewModel.coinMaxAllowedDecimals,
//        rate = viewModel.coinRate,
//        feeCoinRate = viewModel.coinRate,
//        sendResult = viewModel.sendResult,
//        token = confirmationData.token,
//        feeCoin = confirmationData.feeCoin,
//        amount = confirmationData.amount,
//        address = null,
//        contact = confirmationData.contact,
//        fee = viewModel.fee,
//        memo = confirmationData.memo,
//        onClickSend = viewModel::onClickSend,
//        sendEntryPointDestId = sendEntryPointDestId,
//        title = stringResource(R.string.Balance_Zcash_UnshieldedBalance_Shield)
//    )

}
