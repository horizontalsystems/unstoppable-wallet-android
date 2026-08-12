package io.horizontalsystems.walletkit.modules.zcashmigration

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
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.SnackbarDuration
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.confirm.ErrorSheet
import io.horizontalsystems.walletkit.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.walletkit.modules.evmfee.Cautions
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.ConfirmationBottomSection
import io.horizontalsystems.walletkit.modules.send.ConfirmationTopSection
import io.horizontalsystems.walletkit.modules.send.SendResult
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.TextWarningVersion2
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import kotlinx.coroutines.delay
import kotlin.reflect.KClass

@Composable
fun ZcashMigrationScreen(
    navigation: HSNavigation,
    viewModel: ZcashMigrationViewModel,
    entryPointDestId: KClass<out HSPage>
) {
    val view = LocalView.current
    val uiState = viewModel.uiState
    val sendResult = uiState.sendResult

    // Side effects live in the effect, not composition: a recomposition must not repeat the
    // success HUD or stack another error sheet.
    LaunchedEffect(sendResult) {
        when (sendResult) {
            is SendResult.Sent -> {
                HudHelper.showSuccessMessage(
                    view,
                    R.string.Send_Success,
                    SnackbarDuration.LONG
                )
                delay(1200)
                navigation.removeLastUntil(entryPointDestId, true)
            }

            is SendResult.Failed -> {
                navigation.slideFromBottom(
                    ErrorSheet(
                        ErrorSheet.Input(
                            sendResult.caution.getDescription() ?: sendResult.caution.getString()
                        )
                    )
                )
            }

            else -> Unit
        }
    }

    HSScaffold(
        title = stringResource(R.string.Zcash_Migration_Confirm_Title),
        onBack = navigation::removeLastOrNull,
        bottomBar = {
            ButtonsGroupWithShade {
                MigrateButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    sendResult = sendResult,
                    onClick = viewModel::onClickMigrate,
                    // The fee is set exactly when the migration proposal resolves; before that
                    // executeIronwoodMigration() has no proposal to send.
                    enabled = uiState.error == null && uiState.fee != null
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
                navigation = navigation,
                memo = null,
                // The fee only exists once proposeIronwoodMigration() resolves, and
                // ConfirmationBottomSection renders no fee row for a null fee — render the same
                // cell with "n/a" in its place, so only the value changes when the fee lands.
                additionalFields = if (uiState.fee == null) {
                    {
                        DataFieldFeeTemplate(
                            navigation = navigation,
                            primary = stringResource(R.string.NotAvailable),
                            secondary = null,
                            title = stringResource(R.string.FeeSettings_NetworkFee),
                            infoText = stringResource(R.string.FeeSettings_NetworkFee_Info)
                        )
                    }
                } else {
                    null
                }
            )

            VSpacer(16.dp)
            // Migration moves funds across pools through the transparent pool, so the amount is
            // briefly visible on chain — warn before the user confirms.
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
