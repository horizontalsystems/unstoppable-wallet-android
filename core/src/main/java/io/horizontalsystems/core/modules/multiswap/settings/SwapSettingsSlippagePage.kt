package io.horizontalsystems.core.modules.multiswap.settings

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
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.ethereum.CautionViewItem
import io.horizontalsystems.core.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.core.modules.evmfee.Cautions
import io.horizontalsystems.core.modules.evmfee.NumberInputWithButtons
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.modules.nav3.LocalResultEventBus
import io.horizontalsystems.core.serializers.BigDecimalSerializer
import io.horizontalsystems.core.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.ui.compose.TranslatableString
import io.horizontalsystems.core.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.ui.compose.components.MenuItem
import io.horizontalsystems.core.ui.compose.components.VSpacer
import io.horizontalsystems.core.uiv3.components.HSScaffold
import io.horizontalsystems.core.uiv3.components.info.TextBlock
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class SwapSettingsSlippagePage(val input: Input) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SwapSlippageSettingsScreen(navigation, input.slippage)
    }

    @Serializable
    data class Input(@Serializable(with = BigDecimalSerializer::class) val slippage: BigDecimal)

    @Parcelize
    data class Result(val slippage: BigDecimal) : Parcelable
}

@Composable
fun SwapSlippageSettingsScreen(
    navigation: HSNavigation,
    initialSlippage: BigDecimal
) {
    val resultEventBus = LocalResultEventBus.current
    val viewModel = viewModel<SwapTransactionSlippageViewModel>(
        initializer = SwapTransactionSlippageViewModel.init(initialSlippage)
    )
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.SendEvmSettings_SlippageTolerance),
        onBack = navigation::removeLastOrNull,
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
                        resultEventBus.sendResult(
                            SwapSettingsSlippagePage.Result(uiState.slippage)
                        )
                        navigation.removeLastOrNull()
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
