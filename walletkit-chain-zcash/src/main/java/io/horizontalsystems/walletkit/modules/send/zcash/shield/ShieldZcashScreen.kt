package io.horizontalsystems.walletkit.modules.send.zcash.shield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.walletkit.modules.send.SendResult
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.rememberConfirmationData
import io.horizontalsystems.walletkit.modules.send.SendConfirmationScreen
import kotlin.reflect.KClass

@Composable
fun ShieldZcashScreen(
    navigation: HSNavigation,
    viewModel: ShieldZcashViewModel,
    sendEntryPointDestId: KClass<out HSPage>,
    returnsResult: Boolean,
) {
    val confirmationData = rememberConfirmationData(navigation) { viewModel.getConfirmationData() }
        ?: return

    if (returnsResult) {
        val resultEventBus = LocalResultEventBus.current
        LaunchedEffect(viewModel.sendResult) {
            if (viewModel.sendResult is SendResult.Sent) {
                resultEventBus.sendResult(ShieldZcashPage.Result(true))
            }
        }
    }

    SendConfirmationScreen(
        navigation = navigation,
        coinMaxAllowedDecimals = viewModel.coinMaxAllowedDecimals,
        feeCoinMaxAllowedDecimals = viewModel.coinMaxAllowedDecimals,
        rate = viewModel.coinRate,
        feeCoinRate = viewModel.coinRate,
        sendResult = viewModel.sendResult,
        token = confirmationData.token,
        feeCoin = confirmationData.feeCoin,
        amount = confirmationData.amount,
        address = Address(
            hex = viewModel.shieldedAddress,
            domain = stringResource(R.string.Zcash_ToShieldedAddress),
        ),
        contact = confirmationData.contact,
        fee = viewModel.fee,
        memo = confirmationData.memo,
        onClickSend = viewModel::onClickSend,
        sendEntryPointDestId = sendEntryPointDestId,
        title = stringResource(R.string.Zcash_ConfirmShielding),
        sendButtonTitle = stringResource(R.string.Zcash_ShieldZec),
        sendingButtonTitle = stringResource(R.string.Zcash_Shielding),
        onFailureShown = viewModel::onFailureShown,
    )

}
