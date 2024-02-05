package io.horizontalsystems.bankwallet.modules.send.bitcoin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationScreen
import io.horizontalsystems.bankwallet.ui.compose.DisposableLifecycleCallbacks

@Composable
fun SendBitcoinConfirmationScreen(
    navController: NavController,
    sendViewModel: SendBitcoinViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int
) {
    var confirmationData by remember { mutableStateOf(sendViewModel.getConfirmationData()) }
    var refresh by remember { mutableStateOf(false) }

    DisposableLifecycleCallbacks(
        onResume = {
            if (refresh) {
                confirmationData = sendViewModel.getConfirmationData()
            }
        },
        onPause = {
            refresh = true
        }
    )

    SendConfirmationScreen(
        navController = navController,
        coinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        feeCoinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        fiatMaxAllowedDecimals = sendViewModel.fiatMaxAllowedDecimals,
        amountInputType = amountInputModeViewModel.inputType,
        rate = sendViewModel.coinRate,
        feeCoinRate = sendViewModel.coinRate,
        sendResult = sendViewModel.sendResult,
        blockchainType = sendViewModel.blockchainType,
        coin = confirmationData.coin,
        feeCoin = confirmationData.coin,
        amount = confirmationData.amount,
        address = confirmationData.address,
        contact = confirmationData.contact,
        fee = confirmationData.fee,
        lockTimeInterval = confirmationData.lockTimeInterval,
        memo = confirmationData.memo,
        rbfEnabled = confirmationData.rbfEnabled,
        onClickSend = sendViewModel::onClickSend,
        sendEntryPointDestId = sendEntryPointDestId
    )
}