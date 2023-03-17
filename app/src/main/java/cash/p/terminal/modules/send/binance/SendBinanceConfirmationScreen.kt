package cash.p.terminal.modules.send.binance

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.send.SendConfirmationScreen

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
        feeCoinMaxAllowedDecimals = sendViewModel.feeTokenMaxAllowedDecimals,
        fiatMaxAllowedDecimals = sendViewModel.fiatMaxAllowedDecimals,
        amountInputType = amountInputModeViewModel.inputType,
        rate = sendViewModel.coinRate,
        feeCoinRate = sendViewModel.feeCoinRate,
        sendResult = sendViewModel.sendResult,
        blockchainType = sendViewModel.blockchainType,
        coin = confirmationData.coin,
        feeCoin = confirmationData.feeCoin,
        amount = confirmationData.amount,
        address = confirmationData.address,
        contact = confirmationData.contact,
        fee = confirmationData.fee,
        lockTimeInterval = confirmationData.lockTimeInterval,
        memo = confirmationData.memo,
        onClickSend = sendViewModel::onClickSend
    )
}