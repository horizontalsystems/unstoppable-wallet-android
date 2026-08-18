package io.horizontalsystems.walletkit.modules.send.zcash.shield

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.rememberConfirmationData
import io.horizontalsystems.walletkit.modules.send.SendConfirmationScreen
import kotlin.reflect.KClass

@Composable
fun ShieldZcashScreen(
    navigation: HSNavigation,
    viewModel: ShieldZcashViewModel,
    sendEntryPointDestId: KClass<out HSPage>
) {
    val confirmationData = rememberConfirmationData(navigation) { viewModel.getConfirmationData() }
        ?: return

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
        address = null,
        contact = confirmationData.contact,
        fee = viewModel.fee,
        memo = confirmationData.memo,
        onClickSend = viewModel::onClickSend,
        sendEntryPointDestId = sendEntryPointDestId,
        title = stringResource(R.string.Balance_Zcash_UnshieldedBalance_Shield)
    )

}
