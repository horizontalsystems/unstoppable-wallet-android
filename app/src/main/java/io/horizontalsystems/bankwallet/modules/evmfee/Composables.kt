package io.horizontalsystems.bankwallet.modules.evmfee

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
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.multiswap.SwapInfoDialog
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.animations.shake
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.AlertCard
import io.horizontalsystems.bankwallet.uiv3.components.AlertFormat
import io.horizontalsystems.bankwallet.uiv3.components.AlertType
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton
import java.math.BigDecimal

@Composable
fun Eip1559FeeSettings(
    viewModel: Eip1559FeeSettingsViewModel,
    navController: NavController
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
                navController = navController,
                title = stringResource(R.string.FeeSettings_NetworkFee),
                info = stringResource(R.string.FeeSettings_NetworkFee_Info),
                primary = summaryViewItem?.fee?.primary ?: "---",
                secondary = summaryViewItem?.fee?.secondary
            )
            HsDivider()
            FeeField(
                navController = navController,
                title = stringResource(R.string.FeeSettings_GasLimit),
                info = stringResource(R.string.FeeSettings_GasLimit_Info),
                primary = summaryViewItem?.gasLimit ?: "",
            )
            HsDivider()
            FeeField(
                navController = navController,
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
                    navController = navController,
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
                    navController = navController,
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
fun EvmSettingsInput(
    title: String,
    info: String,
    value: BigDecimal,
    decimals: Int,
    warnings: List<Warning>,
    errors: List<Throwable>,
    navController: NavController,
    onValueChange: (BigDecimal) -> Unit,
    onClickIncrement: () -> Unit,
    onClickDecrement: () -> Unit
) {
    val textColor = when {
        errors.isNotEmpty() -> ComposeAppTheme.colors.lucian
        warnings.isNotEmpty() -> ComposeAppTheme.colors.jacob
        else -> ComposeAppTheme.colors.leah
    }

    EvmSettingsInput(
        title = title,
        info = info,
        value = value,
        decimals = decimals,
        textColor = textColor,
        navController = navController,
        onValueChange = onValueChange,
        onClickIncrement = onClickIncrement,
        onClickDecrement = onClickDecrement
    )
}

@Composable
fun EvmSettingsInput(
    title: String,
    info: String,
    value: BigDecimal,
    decimals: Int,
    caution: HSCaution?,
    navController: NavController,
    onValueChange: (BigDecimal) -> Unit,
    onClickIncrement: () -> Unit,
    onClickDecrement: () -> Unit
) {
    val borderColor = when (caution?.type) {
        HSCaution.Type.Error -> ComposeAppTheme.colors.red50
        HSCaution.Type.Warning -> ComposeAppTheme.colors.yellow50
        else -> ComposeAppTheme.colors.blade
    }

    EvmSettingsInput(
        title = title,
        info = info,
        value = value,
        decimals = decimals,
        textColor = borderColor,
        navController = navController,
        onValueChange = onValueChange,
        onClickIncrement = onClickIncrement,
        onClickDecrement = onClickDecrement
    )
}

@Composable
private fun EvmSettingsInput(
    title: String,
    info: String,
    value: BigDecimal,
    decimals: Int,
    textColor: Color,
    navController: NavController,
    onValueChange: (BigDecimal) -> Unit,
    onClickIncrement: () -> Unit,
    onClickDecrement: () -> Unit,
) {
    HeaderText(text = title) {
        navController.slideFromBottom(
            R.id.feeSettingsInfoDialog,
            FeeSettingsInfoDialog.Input(title, info)
        )
    }

    NumberInputWithButtons(
        value,
        decimals,
        textColor,
        onValueChange,
        onClickIncrement,
        onClickDecrement
    )
}

@Composable
private fun NumberInputWithButtons(
    value: BigDecimal,
    decimals: Int,
    textColor: Color,
    onValueChange: (BigDecimal) -> Unit,
    onClickIncrement: () -> Unit,
    onClickDecrement: () -> Unit
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = value.toString()
        mutableStateOf(TextFieldValue(text))
    }
    var playShakeAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        textState = textState.copy(text = value.toString(), selection = TextRange("$value".length))
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence),
        verticalAlignment = Alignment.CenterVertically
    ) {

        BasicTextField(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .weight(1f)
                .shake(
                    enabled = playShakeAnimation,
                    onAnimationFinish = { playShakeAnimation = false }
                ),
            value = textState,
            onValueChange = { textFieldValue ->
                val newValue = textFieldValue.text.toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (newValue.scale() <= decimals) {
                    val currentText = textState.text
                    textState = textFieldValue
                    if (currentText != textFieldValue.text) {
                        onValueChange(newValue)
                    }
                } else {
                    playShakeAnimation = true
                }
            },
            textStyle = ColoredTextStyle(
                color = textColor,
                textStyle = ComposeAppTheme.typography.body
            ),
            singleLine = true,
            cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        HSIconButton(
            icon = painterResource(id = R.drawable.ic_minus_20),
            variant = ButtonVariant.Secondary,
            size = ButtonSize.Small,
            onClick =onClickDecrement
        )
        HSpacer(16.dp)
        HSIconButton(
            icon = painterResource(id = R.drawable.ic_plus_20),
            variant = ButtonVariant.Secondary,
            size = ButtonSize.Small,
            onClick =onClickIncrement
        )
        HSpacer(16.dp)
    }
}

@Composable
fun ButtonsGroupWithShade(
    ButtonsContent: @Composable (() -> Unit)
) {
    Column(
        modifier = Modifier
            .offset(y = -(24.dp))
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(ComposeAppTheme.colors.transparent, ComposeAppTheme.colors.tyler)
                    )
                )
        )
        Box(
            modifier = Modifier.background(ComposeAppTheme.colors.tyler)
        ) {
            ButtonsContent()
        }
    }
}

@Composable
fun LegacyFeeSettings(
    viewModel: LegacyFeeSettingsViewModel,
    navController: NavController
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
                navController = navController,
                title = stringResource(R.string.FeeSettings_NetworkFee),
                info = stringResource(R.string.FeeSettings_NetworkFee_Info),
                primary = summaryViewItem?.fee?.primary ?: "---",
                secondary = summaryViewItem?.fee?.secondary
            )
            HsDivider()
            FeeField(
                navController = navController,
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
                navController = navController,
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
fun Cautions(cautions: List<CautionViewItem>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        cautions.forEach { caution ->
            val alertType = when (caution.type) {
                CautionViewItem.Type.Error -> AlertType.Critical
                CautionViewItem.Type.Warning -> AlertType.Caution
            }

            AlertCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                format = AlertFormat.Structured,
                type = alertType,
                text = caution.text,
                titleCustom = caution.title
            )
        }
    }
}

@Composable
private fun FeeField(
    navController: NavController,
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
                    navController.slideFromBottom(
                        R.id.swapInfoDialog,
                        SwapInfoDialog.Input(title, info)
                    )
                }
            )
        },
        right = {
            CellRightInfo(
                titleSubheadSb = primary.hs,
                description = secondary?.hs
            )
        },
    )
}
