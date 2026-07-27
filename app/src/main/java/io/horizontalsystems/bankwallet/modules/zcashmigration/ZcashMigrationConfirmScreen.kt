package io.horizontalsystems.bankwallet.modules.zcashmigration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.send.ConfirmationBottomSection
import io.horizontalsystems.bankwallet.modules.send.ConfirmationTopSection
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextWarningVersion2
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.modules.confirm.ErrorBottomSheet
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

@Composable
fun ZcashMigrationConfirmScreen(
    navController: NavController,
    viewModel: ZcashMigrationViewModel
) {
    val view = LocalView.current
    val uiState = viewModel.uiState
    val sendResult = uiState.sendResult
    // Resolved during composition: caution strings are @Composable and cannot be
    // read inside the effect coroutine
    val failedMessage = (sendResult as? SendResult.Failed)?.caution?.let {
        it.getDescription() ?: it.getString()
    }

    LaunchedEffect(sendResult) {
        when (sendResult) {
            is SendResult.Sent -> {
                HudHelper.showSuccessMessage(
                    view,
                    R.string.Send_Success,
                    SnackbarDuration.LONG
                )
                delay(1200)
                navController.popBackStack()
            }

            is SendResult.Failed -> {
                navController.slideFromBottom(
                    R.id.errorBottomSheet,
                    ErrorBottomSheet.Input(failedMessage ?: "")
                )
            }

            else -> Unit
        }
    }

    HSScaffold(
        title = stringResource(R.string.Zcash_Migration_Confirm_Title),
        onBack = navController::popBackStack,
        bottomBar = {
            ButtonsGroupWithShade {
                MigrateButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    sendResult = sendResult,
                    onClick = viewModel::onClickMigrate,
                    enabled = uiState.error == null
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 106.dp)
        ) {
            VSpacer(16.dp)
            ConfirmationTopSection(
                token = viewModel.wallet.token,
                amount = uiState.amount,
                coinMaxAllowedDecimals = viewModel.coinMaxAllowedDecimals,
                rate = uiState.coinRate,
                address = null,
                contact = null,
            )

            ConfirmationBottomSection(
                feeCoin = viewModel.wallet.coin,
                feeCoinMaxAllowedDecimals = viewModel.coinMaxAllowedDecimals,
                fee = uiState.fee,
                feeCoinRate = uiState.coinRate,
                navController = navController,
                memo = null
            )

            VSpacer(16.dp)
            TextWarningVersion2(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.Zcash_Migration_PubliclyVisible_Title),
                text = stringResource(R.string.Zcash_Migration_PubliclyVisible_Description),
                icon = R.drawable.ic_warning_filled_20
            )

            uiState.error?.let {
                Cautions(listOf(CautionViewItem.fromThrowable(it)))
            }
        }
    }
}

@Composable
private fun MigrateButton(
    modifier: Modifier,
    sendResult: SendResult?,
    onClick: () -> Unit,
    enabled: Boolean
) {
    when (sendResult) {
        SendResult.Sending -> {
            ButtonPrimaryYellow(
                modifier = modifier,
                title = stringResource(R.string.Send_Sending),
                onClick = { },
                enabled = false
            )
        }

        is SendResult.Sent -> {
            ButtonPrimaryYellow(
                modifier = modifier,
                title = stringResource(R.string.Send_Success),
                onClick = { },
                enabled = false
            )
        }

        else -> {
            ButtonPrimaryYellow(
                modifier = modifier,
                title = stringResource(R.string.Balance_Zcash_Migration_Migrate),
                onClick = onClick,
                enabled = enabled
            )
        }
    }
}
