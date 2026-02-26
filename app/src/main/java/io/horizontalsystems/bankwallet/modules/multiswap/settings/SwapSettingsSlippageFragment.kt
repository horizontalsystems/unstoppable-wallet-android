package io.horizontalsystems.bankwallet.modules.multiswap.settings

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.evmfee.NumberInputWithButtons
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data object SwapSettingsSlippageScreen : HSScreen()

class SwapSettingsSlippageFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            SwapSlippageSettingsScreen(navController, input.slippage)
        }
    }

    @Parcelize
    data class Input(val slippage: BigDecimal) : Parcelable

    @Parcelize
    data class Result(val slippage: BigDecimal) : Parcelable
}

@Composable
fun SwapSlippageSettingsScreen(
    navController: NavController,
    initialSlippage: BigDecimal
) {
    val viewModel = viewModel<SwapTransactionSlippageViewModel>(
        initializer = SwapTransactionSlippageViewModel.init(initialSlippage)
    )
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.SendEvmSettings_SlippageTolerance),
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Reset),
                enabled = uiState.resetEnabled,
                onClick = {
                    viewModel.onReset()
                },
                tint = ComposeAppTheme.colors.jacob
            )
        ),
        bottomBar = {
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(id = R.string.Button_Apply),
                    enabled = uiState.applyEnabled,
                    onClick = {
                        navController.setNavigationResultX(
                            SwapSettingsSlippageFragment.Result(uiState.slippage)
                        )
                        navController.popBackStack()
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            TextBlock(
                text = stringResource(R.string.SendEvmSettings_SlippageTolerance_Info),
            )
            VSpacer(8.dp)

            val textColor = when (uiState.caution?.type) {
                CautionViewItem.Type.Error -> ComposeAppTheme.colors.lucian
                CautionViewItem.Type.Warning -> ComposeAppTheme.colors.jacob
                else -> ComposeAppTheme.colors.leah
            }

            NumberInputWithButtons(
                value = uiState.slippage,
                decimals = SwapTransactionSlippageViewModel.DECIMALS,
                textColor = textColor,
                onValueChange = { newValue ->
                    viewModel.onSlippageChange(newValue)
                },
                onClickIncrement = {
                    viewModel.onIncrement()
                },
                onClickDecrement = {
                    viewModel.onDecrement()
                }
            )

            uiState.caution?.let {
                Cautions(listOf(it))
            }

            VSpacer(32.dp)
        }
    }
}
