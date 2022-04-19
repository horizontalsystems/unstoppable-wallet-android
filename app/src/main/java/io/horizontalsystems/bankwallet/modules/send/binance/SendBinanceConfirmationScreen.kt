package io.horizontalsystems.bankwallet.modules.send.binance

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationScreen

@Composable
fun SendBinanceConfirmationScreen(
    navController: NavController,
    sendViewModel: SendBinanceViewModel,
    amountInputModeViewModel: AmountInputModeViewModel
) {
    val confirmationData = sendViewModel.getConfirmationData()

    SendConfirmationScreen(
        navController = navController,
        coinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        feeCoinMaxAllowedDecimals = sendViewModel.feeCoinMaxAllowedDecimals,
        fiatMaxAllowedDecimals = sendViewModel.fiatMaxAllowedDecimals,
        amountInputType = amountInputModeViewModel.inputType,
        rate = sendViewModel.coinRate,
        feeCoinRate = sendViewModel.feeCoinRate,
        sendResult = sendViewModel.sendResult,
        coin = confirmationData.coin,
        feeCoin = confirmationData.feeCoin,
        amount = confirmationData.amount,
        address = confirmationData.address,
        fee = confirmationData.fee,
        lockTimeInterval = confirmationData.lockTimeInterval,
        memo = confirmationData.memo,
        onClickSend = sendViewModel::onClickSend
    )
}