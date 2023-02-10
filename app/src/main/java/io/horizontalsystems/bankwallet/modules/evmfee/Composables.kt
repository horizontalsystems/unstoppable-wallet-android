package io.horizontalsystems.bankwallet.modules.evmfee

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559FeeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.animations.shake
import io.horizontalsystems.bankwallet.ui.compose.components.*
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
    val cautions = viewModel.cautions

    Column {
        Spacer(modifier = Modifier.height(12.dp))
        CellSingleLineLawrenceSection(
            listOf(
                {
                    MaxFeeCell(
                        title = stringResource(R.string.FeeSettings_Fee),
                        value = summaryViewItem?.fee ?: "",
                        viewState = summaryViewItem?.viewState,
                        navController = navController
                    )
                },
                {
                    FeeInfoCell(
                        title = stringResource(R.string.FeeSettings_GasLimit),
                        value = summaryViewItem?.gasLimit,
                        infoTitle = Translator.getString(R.string.FeeSettings_GasLimit),
                        infoText = Translator.getString(R.string.FeeSettings_GasLimit_Info),
                        navController = navController
                    )
                },
                {
                    FeeInfoCell(
                        title = stringResource(R.string.FeeSettings_BaseFee),
                        value = currentBaseFee,
                        infoTitle = Translator.getString(R.string.FeeSettings_BaseFee),
                        infoText = Translator.getString(R.string.FeeSettings_BaseFee_Info),
                        navController = navController
                    )
                }
            )
        )

        maxFeeViewItem?.let { maxFee ->
            priorityFeeViewItem?.let { priorityFee ->

                Spacer(modifier = Modifier.height(24.dp))
                EvmSettingsInput(
                    title = stringResource(R.string.FeeSettings_MaxFee),
                    info = stringResource(R.string.FeeSettings_MaxFee_Info),
                    value = BigDecimal(maxFee.weiValue).divide(BigDecimal(maxFee.scale.scaleValue)),
                    decimals = maxFee.scale.decimals,
                    navController = navController,
                    onValueChange = {
                        viewModel.onSelectGasPrice(maxFee.wei(it), priorityFee.weiValue)
                    },
                    onClickIncrement = {
                        viewModel.onIncrementBaseFee(maxFee.weiValue, priorityFee.weiValue)
                    },
                    onClickDecrement = {
                        viewModel.onDecrementBaseFee(maxFee.weiValue, priorityFee.weiValue)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
                EvmSettingsInput(
                    title = stringResource(R.string.FeeSettings_MaxMinerTips),
                    info = stringResource(R.string.FeeSettings_MaxMinerTips_Info),
                    value = BigDecimal(priorityFee.weiValue).divide(BigDecimal(priorityFee.scale.scaleValue)),
                    decimals = priorityFee.scale.decimals,
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

        Cautions(cautions)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun EvmSettingsInput(
    title: String,
    info: String,
    value: BigDecimal,
    decimals: Int,
    state: DataState<Any>? = null,
    navController: NavController,
    onValueChange: (BigDecimal) -> Unit,
    onClickIncrement: () -> Unit,
    onClickDecrement: () -> Unit
) {
    HeaderText(text = title) {
        navController.slideFromBottom(
            R.id.feeSettingsInfoDialog,
            FeeSettingsInfoDialog.prepareParams(title, info)
        )
    }
    NumberInputWithButtons(value, decimals, state, onValueChange, onClickIncrement, onClickDecrement)
}

@Composable
private fun NumberInputWithButtons(
    value: BigDecimal,
    decimals: Int,
    state: DataState<Any>? = null,
    onValueChange: (BigDecimal) -> Unit,
    onClickIncrement: () -> Unit,
    onClickDecrement: () -> Unit
) {
    val borderColor = when (state) {
        is DataState.Error -> ComposeAppTheme.colors.red50
        else -> ComposeAppTheme.colors.steel20
    }

    var textState by rememberSaveable(value, stateSaver = TextFieldValue.Saver) {
        val text = value.toString()
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    var playShakeAnimation by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
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
                    textState = textFieldValue
                    onValueChange(newValue)
                } else {
                    playShakeAnimation = true
                }
            },
            singleLine = true,
            cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        ButtonSecondaryCircle(
            modifier = Modifier.padding(end = 16.dp),
            icon = R.drawable.ic_minus_20,
            onClick = onClickDecrement
        )

        ButtonSecondaryCircle(
            modifier = Modifier.padding(end = 16.dp),
            icon = R.drawable.ic_plus_20,
            onClick = onClickIncrement
        )
    }

    if (state is DataState.Error) {
        caption_lucian(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
            text = state.error.message ?: state.error.javaClass.simpleName,
        )
    }
}

@Composable
fun ButtonsGroupWithShade(
    ButtonsContent: @Composable (() -> Unit)
) {
    Column(
        modifier = Modifier.offset(y = -(24.dp))
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
            modifier = Modifier
                .background(ComposeAppTheme.colors.tyler)
                .padding(bottom = 8.dp) // With 24dp offset actual padding will be 32dp
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
    val cautions = viewModel.cautions

    Column {
        Spacer(modifier = Modifier.height(12.dp))
        CellSingleLineLawrenceSection(
            listOf(
                {
                    MaxFeeCell(
                        title = stringResource(R.string.FeeSettings_MaxFee),
                        value = summaryViewItem?.fee ?: "",
                        viewState = summaryViewItem?.viewState,
                        navController = navController
                    )
                },
                {
                    FeeInfoCell(
                        title = stringResource(R.string.FeeSettings_GasLimit),
                        value = summaryViewItem?.gasLimit,
                        infoTitle = Translator.getString(R.string.FeeSettings_GasLimit),
                        infoText = Translator.getString(R.string.FeeSettings_GasLimit_Info),
                        navController = navController
                    )
                }
            )
        )

        viewItem?.let { fee ->
            Spacer(modifier = Modifier.height(24.dp))
            EvmSettingsInput(
                title = stringResource(R.string.FeeSettings_GasPrice),
                info = stringResource(R.string.FeeSettings_GasPrice_Info),
                value = BigDecimal(fee.weiValue).divide(BigDecimal(fee.scale.scaleValue)),
                decimals = fee.scale.decimals,
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

        Cautions(cautions)

        Spacer(modifier = Modifier.height(32.dp))
    }

}

@Composable
fun Cautions(cautions: List<CautionViewItem>) {
    val modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        cautions.forEach { caution ->

            when (caution.type) {
                CautionViewItem.Type.Error -> {
                    TextImportantError(
                        modifier = modifier,
                        text = caution.text,
                        title = caution.title,
                        icon = R.drawable.ic_attention_20
                    )
                }
                CautionViewItem.Type.Warning -> {
                    TextImportantWarning(
                        modifier = modifier,
                        text = caution.text,
                        title = caution.title,
                        icon = R.drawable.ic_attention_20
                    )
                }
            }
        }
    }
}

@Composable
fun FeeInfoCell(
    title: String,
    value: String?,
    infoTitle: String,
    infoText: String,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                navController.slideFromBottom(
                    R.id.feeSettingsInfoDialog,
                    FeeSettingsInfoDialog.prepareParams(infoTitle, infoText)
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.padding(end = 16.dp),
            painter = painterResource(id = R.drawable.ic_info_20), contentDescription = ""
        )
        subhead1_grey(text = title)
        subhead1_leah(
            modifier = Modifier.weight(1f),
            text = value ?: "",
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EvmFeeCell(
    title: String,
    value: String?,
    loading: Boolean,
    viewState: ViewState?,
    highlightEditButton: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    CellUniversalLawrenceSection(
        listOf {
            HSFeeCell(
                title = title,
                value = value,
                loading = loading,
                viewState = viewState,
                highlightEditButton = highlightEditButton,
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            )
        })
}

@Composable
fun HSFeeCell(
    title: String,
    value: String?,
    loading: Boolean,
    viewState: ViewState?,
    highlightEditButton: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = if (enabled) onClick else null,
    ) {
        subhead2_grey(text = title)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = ComposeAppTheme.colors.grey,
                    strokeWidth = 1.5.dp
                )
            } else {
                val color = if (viewState is ViewState.Error) {
                    ComposeAppTheme.colors.lucian
                } else if (value == null) {
                    ComposeAppTheme.colors.grey50
                } else {
                    ComposeAppTheme.colors.leah
                }

                Text(
                    text = value ?: stringResource(R.string.NotAvailable),
                    style = ComposeAppTheme.typography.subhead1,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (enabled) {
            Box(modifier = Modifier.padding(start = 8.dp)) {
                val tintColor = if (highlightEditButton)
                    ComposeAppTheme.colors.jacob
                else
                    ComposeAppTheme.colors.grey

                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.ic_edit_20),
                    colorFilter = ColorFilter.tint(tintColor),
                    contentDescription = ""
                )
            }
        }
    }
}


@Composable
fun MaxFeeCell(
    title: String,
    value: String,
    viewState: ViewState?,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                navController.slideFromBottom(
                    R.id.feeSettingsInfoDialog,
                    FeeSettingsInfoDialog.prepareParams(
                        Translator.getString(R.string.FeeSettings_Fee),
                        Translator.getString(R.string.FeeSettings_Fee_Info)
                    )
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.padding(end = 16.dp),
            painter = painterResource(id = R.drawable.ic_info_20), contentDescription = ""
        )
        subhead2_grey(text = title)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            if (viewState == ViewState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = ComposeAppTheme.colors.grey,
                    strokeWidth = 1.5.dp
                )
            } else {
                Text(
                    text = value,
                    style = ComposeAppTheme.typography.subhead1,
                    color = if (viewState is ViewState.Error) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.leah,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
