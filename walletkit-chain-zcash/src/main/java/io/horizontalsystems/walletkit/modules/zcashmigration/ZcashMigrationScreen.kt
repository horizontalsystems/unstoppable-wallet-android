package io.horizontalsystems.walletkit.modules.zcashmigration

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.SendConfirmationScreen
import io.horizontalsystems.walletkit.ui.compose.components.TextWarningVersion2
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import kotlin.reflect.KClass

@Composable
fun ZcashMigrationScreen(
    navigation: HSNavigation,
    viewModel: ZcashMigrationViewModel,
    entryPointDestId: KClass<out HSPage>
) {
    val uiState = viewModel.uiState

    SendConfirmationScreen(
        navigation = navigation,
        coinMaxAllowedDecimals = viewModel.coinMaxAllowedDecimals,
        feeCoinMaxAllowedDecimals = viewModel.coinMaxAllowedDecimals,
        rate = uiState.coinRate,
        feeCoinRate = uiState.coinRate,
        sendResult = uiState.sendResult,
        token = viewModel.wallet.token,
        feeCoin = viewModel.wallet.coin,
        amount = uiState.amount,
        address = null,
        contact = null,
        fee = uiState.fee,
        memo = null,
        onClickSend = viewModel::onClickMigrate,
        sendEntryPointDestId = entryPointDestId,
        title = stringResource(R.string.Zcash_Migration_Confirm_Title),
        error = uiState.error,
        additionalFields = {
            VSpacer(16.dp)
            // Migration moves funds across pools through the transparent pool, so the amount is
            // briefly visible on chain — warn before the user confirms.
            TextWarningVersion2(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.Zcash_Migration_PubliclyVisible_Title),
                text = stringResource(R.string.Zcash_Migration_PubliclyVisible_Description),
                icon = R.drawable.ic_warning_filled_20
            )
        }
    )
}
