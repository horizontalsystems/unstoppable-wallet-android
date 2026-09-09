package io.horizontalsystems.walletkit.modules.send.xrp

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.SendConfirmationScreen
import io.horizontalsystems.walletkit.modules.send.rememberConfirmationData
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import kotlin.reflect.KClass

@Composable
fun SendXrpConfirmationScreen(
    navigation: HSNavigation,
    sendViewModel: SendXrpViewModel,
    sendEntryPointDestId: KClass<out HSPage>
) {
    val confirmationData = rememberConfirmationData(navigation) { sendViewModel.getConfirmationData() }
        ?: return
    val destinationTag = sendViewModel.destinationTag

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
        sendEntryPointDestId = sendEntryPointDestId,
        additionalFields = destinationTag?.let { tag ->
            {
                QuoteInfoRow(
                    title = stringResource(R.string.Send_DestinationTag),
                    value = tag.toString().hs(color = ComposeAppTheme.colors.leah),
                )
            }
        }
    )
}
