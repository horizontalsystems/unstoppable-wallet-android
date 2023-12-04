package io.horizontalsystems.bankwallet.modules.send.evm.settings

import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.evmfee.Eip1559FeeSettings
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeModule
import io.horizontalsystems.bankwallet.modules.evmfee.EvmSettingsInput
import io.horizontalsystems.bankwallet.modules.evmfee.LegacyFeeSettings
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class SendEvmSettingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.requireInput<Input>()
        val navGraphId = input.navGraphId
        val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(navGraphId)
        val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(navGraphId)
        val sendViewModel by navGraphViewModels<SendEvmTransactionViewModel>(navGraphId)

        val feeSettingsViewModel = viewModel<ViewModel>(
            factory = EvmFeeModule.Factory(
                feeViewModel.feeService,
                feeViewModel.gasPriceService,
                feeViewModel.coinService
            )
        )
        val sendSettingsViewModel = viewModel<SendEvmSettingsViewModel>(
            factory = SendEvmSettingsModule.Factory(sendViewModel.service.settingsService, feeViewModel.coinService)
        )
        SendEvmFeeSettingsScreen(
            viewModel = sendSettingsViewModel,
            feeSettingsViewModel = feeSettingsViewModel,
            nonceViewModel = nonceViewModel,
            navController = navController
        )
    }

    @Parcelize
    data class Input(@IdRes val navGraphId: Int) : Parcelable
}


@Composable
fun SendEvmFeeSettingsScreen(
    viewModel: SendEvmSettingsViewModel,
    feeSettingsViewModel: ViewModel,
    nonceViewModel: SendEvmNonceViewModel,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .background(color = ComposeAppTheme.colors.tyler)
    ) {
        AppBar(
            title = stringResource(R.string.SendEvmSettings_Title),
            navigationIcon = {
                HsIconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "back button",
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Reset),
                    enabled = !viewModel.isRecommendedSettingsSelected,
                    onClick = { viewModel.onClickReset() }
                )
            )
        )

        when (feeSettingsViewModel) {
            is LegacyFeeSettingsViewModel -> {
                LegacyFeeSettings(feeSettingsViewModel, navController)
            }

            is Eip1559FeeSettingsViewModel -> {
                Eip1559FeeSettings(feeSettingsViewModel, navController)
            }
        }

        val nonceUiState = nonceViewModel.uiState
        if (nonceUiState.showInSettings) {
            Spacer(modifier = Modifier.height(24.dp))
            EvmSettingsInput(
                title = stringResource(id = R.string.SendEvmSettings_Nonce),
                info = stringResource(id = R.string.SendEvmSettings_Nonce_Info),
                value = nonceUiState.nonce?.toBigDecimal() ?: BigDecimal.ZERO,
                decimals = 0,
                navController = navController,
                warnings = nonceUiState.warnings,
                errors = nonceUiState.errors,
                onValueChange = {
                    nonceViewModel.onEnterNonce(it.toLong())
                },
                onClickIncrement = nonceViewModel::onIncrementNonce,
                onClickDecrement = nonceViewModel::onDecrementNonce
            )
        }

        Cautions(viewModel.cautions)

        Spacer(modifier = Modifier.height(32.dp))
    }
}
