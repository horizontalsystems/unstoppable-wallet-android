package io.horizontalsystems.walletkit.modules.send.thorchain

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.rememberConfirmationData
import io.horizontalsystems.walletkit.modules.send.SendConfirmationScreen
import kotlin.reflect.KClass

@Composable
fun SendThorchainConfirmationScreen(
    navigation: HSNavigation,
    sendViewModel: SendThorchainViewModel,
    sendEntryPointDestId: KClass<out HSPage>
) {
    val confirmationData = rememberConfirmationData(navigation) { sendViewModel.getConfirmationData() }
        ?: return

    SendConfirmationScreen(
        navigation = navigation,
        coinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        feeCoinMaxAllowedDecimals = sendViewModel.feeTokenMaxAllowedDecimals,
        rate = sendViewModel.coinRate,
        feeCoinRate = sendViewModel.feeCoinRate,
        sendResult = sendViewModel.sendResult,
        token = confirmationData.token,
        feeCoin = confirmationData.feeCoin,
        amount = confirmationData.amount,
        address = confirmationData.address,
        contact = confirmationData.contact,
        fee = confirmationData.fee,
        memo = confirmationData.memo,
        onClickSend = sendViewModel::onClickSend,
        sendEntryPointDestId = sendEntryPointDestId
    )
}
