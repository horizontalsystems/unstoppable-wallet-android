package io.horizontalsystems.walletkit.modules.evmfee

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.core.Warning
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import io.horizontalsystems.walletkit.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ColoredTextStyle
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.animations.shake
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HeaderText
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSIconButton
import java.math.BigDecimal

@Composable
fun Eip1559FeeSettings(
    viewModel: Eip1559FeeSettingsViewModel,
    navigation: HSNavigation
) {
    val summaryViewItem = viewModel.feeSummaryViewItem
    val currentBaseFee = viewModel.currentBaseFee
    val maxFeeViewItem = viewModel.maxFeeViewItem
    val priorityFeeViewItem = viewModel.priorityFeeViewItem

    Column {
        VSpacer(12.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            FeeField(
                navigation = navigation,
                title = stringResource(R.string.FeeSettings_NetworkFee),
                info = stringResource(R.string.FeeSettings_NetworkFee_Info),
                primary = summaryViewItem?.fee?.primary ?: "---",
                secondary = summaryViewItem?.fee?.secondary
            )
            HsDivider()
            FeeField(
                navigation = navigation,
                title = stringResource(R.string.FeeSettings_GasLimit),
                info = stringResource(R.string.FeeSettings_GasLimit_Info),
                primary = summaryViewItem?.gasLimit ?: "",
            )
            HsDivider()
            FeeField(
                navigation = navigation,
                title = stringResource(R.string.FeeSettings_BaseFee),
                info = stringResource(R.string.FeeSettings_BaseFee_Info),
                primary = currentBaseFee ?: "",
            )
        }

        maxFeeViewItem?.let { maxFee ->
            priorityFeeViewItem?.let { priorityFee ->

                EvmSettingsInput(
                    title = stringResource(R.string.FeeSettings_MaxFee),
                    info = stringResource(R.string.FeeSettings_MaxFee_Info),
                    value = BigDecimal(maxFee.weiValue).divide(BigDecimal(maxFee.scale.scaleValue)),
                    decimals = maxFee.scale.decimals,
                    warnings = maxFee.warnings,
                    errors = maxFee.errors,
                    navigation = navigation,
                    onValueChange = {
                        viewModel.onSelectGasPrice(maxFee.wei(it), priorityFee.weiValue)
                    },
                    onClickIncrement = {
                        viewModel.onIncrementMaxFee(maxFee.weiValue, priorityFee.weiValue)
                    },
                    onClickDecrement = {
                        viewModel.onDecrementMaxFee(maxFee.weiValue, priorityFee.weiValue)
                    }
                )

                EvmSettingsInput(
                    title = stringResource(R.string.FeeSettings_MaxMinerTips),
                    info = stringResource(R.string.FeeSettings_MaxMinerTips_Info),
                    value = BigDecimal(priorityFee.weiValue).divide(BigDecimal(priorityFee.scale.scaleValue)),
                    decimals = priorityFee.scale.decimals,
                    warnings = priorityFee.warnings,
                    errors = priorityFee.errors,
                    navigation = navigation,
                    onValueChange = {
                        viewModel.onSelectGasPrice(maxFee.weiValue, priorityFee.wei(it))
                    },
                    onClickIncrement = {
                        viewModel.onIncrementPriorityFee(maxFee.weiValue, priorityFee.weiValue)
                    },
                    onClickDecrement = {
                        viewModel.onDecrementPriorityFee(maxFee.weiValue, priorityFee.weiValue)
                    }
                )
            }
        }
    }
}






@Composable
fun LegacyFeeSettings(
    viewModel: LegacyFeeSettingsViewModel,
    navigation: HSNavigation
) {
    val summaryViewItem = viewModel.feeSummaryViewItem
    val viewItem = viewModel.feeViewItem

    Column {
        VSpacer(12.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            FeeField(
                navigation = navigation,
                title = stringResource(R.string.FeeSettings_NetworkFee),
                info = stringResource(R.string.FeeSettings_NetworkFee_Info),
                primary = summaryViewItem?.fee?.primary ?: "---",
                secondary = summaryViewItem?.fee?.secondary
            )
            HsDivider()
            FeeField(
                navigation = navigation,
                title = stringResource(R.string.FeeSettings_GasLimit),
                info = stringResource(R.string.FeeSettings_GasLimit_Info),
                primary = summaryViewItem?.gasLimit ?: "---",
            )
        }

        viewItem?.let { fee ->
            EvmSettingsInput(
                title = stringResource(R.string.FeeSettings_GasPrice),
                info = stringResource(R.string.FeeSettings_GasPrice_Info),
                value = BigDecimal(fee.weiValue).divide(BigDecimal(fee.scale.scaleValue)),
                decimals = fee.scale.decimals,
                warnings = fee.warnings,
                errors = fee.errors,
                navigation = navigation,
                onValueChange = {
                    viewModel.onSelectGasPrice(fee.wei(it))
                },
                onClickIncrement = {
                    viewModel.onIncrementGasPrice(fee.weiValue)
                },
                onClickDecrement = {
                    viewModel.onDecrementGasPrice(fee.weiValue)
                }
            )
        }
    }
}


@Composable
private fun FeeField(
    navigation: HSNavigation,
    primary: String,
    secondary: String? = null,
    title: String,
    info: String,
) {
    CellPrimary(
        middle = {
            CellMiddleInfoTextIcon(
                text = title.hs(color = ComposeAppTheme.colors.grey),
                icon = painterResource(R.drawable.ic_info_filled_20),
                iconTint = ComposeAppTheme.colors.grey,
                onIconClick = {
                    navigation.slideFromBottom(
                        SwapInfoSheet(SwapInfoSheet.Input(title, info))
                    )
                }
            )
        },
        right = {
            CellRightInfo(
                eyebrow = primary.hs(color = ComposeAppTheme.colors.leah),
                subtitle = secondary?.hs
            )
        },
    )
}
