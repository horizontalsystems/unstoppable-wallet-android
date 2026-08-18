package io.horizontalsystems.walletkit.modules.send.zcash

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.rememberConfirmationData
import io.horizontalsystems.walletkit.modules.send.SendConfirmationScreen
import kotlin.reflect.KClass

@Composable
fun SendZCashConfirmationScreen(
    navigation: HSNavigation,
    sendViewModel: SendZCashViewModel,
    sendEntryPointDestId: KClass<out HSPage>
) {
    val confirmationData = rememberConfirmationData(navigation) { sendViewModel.getConfirmationData() }
        ?: return

    SendConfirmationScreen(
        navigation = navigation,
        coinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        feeCoinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        rate = sendViewModel.coinRate,
        feeCoinRate = sendViewModel.coinRate,
        sendResult = sendViewModel.sendResult,
        token = confirmationData.token,
        feeCoin = confirmationData.feeCoin,
        amount = confirmationData.amount,
        address = confirmationData.address,
        contact = confirmationData.contact,
        fee = confirmationData.fee,
        memo = confirmationData.memo,
        onClickSend = sendViewModel::onClickSend,
        sendEntryPointDestId = sendEntryPointDestId,
        error = confirmationData.error
    )
}