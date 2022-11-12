package io.horizontalsystems.bankwallet.modules.swap.coincard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinFragment
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.getNavigationResult
import java.math.BigDecimal

@Composable
fun SwapCoinCardViewNew(
    modifier: Modifier = Modifier,
    viewModel: SwapCoinCardViewModel,
    uuid: Long,
    amountEnabled: Boolean,
    navController: NavController,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val token by viewModel.tokenCodeLiveData().observeAsState()

    Row(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 8.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, radius = 40.dp),
                    onClick = {
                        navController.getNavigationResult(SelectSwapCoinFragment.resultBundleKey) { bundle ->
                            val requestId = bundle.getLong(SelectSwapCoinFragment.requestIdKey)
                            val coinBalanceItem = bundle.getParcelable<SwapMainModule.CoinBalanceItem>(
                                SelectSwapCoinFragment.coinBalanceItemResultKey
                            )
                            if (requestId == uuid && coinBalanceItem != null) {
                                viewModel.onSelectCoin(coinBalanceItem.token)
                            }
                        }

                        val params = SelectSwapCoinFragment.prepareParams(uuid, viewModel.dex)
                        navController.slideFromBottom(R.id.selectSwapCoinDialog, params)
                    }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinImage(
                modifier = Modifier.size(32.dp),
                iconUrl = token?.coin?.iconUrl,
                placeholder = token?.iconPlaceholder ?: R.drawable.coin_placeholder
            )
            Spacer(Modifier.width(8.dp))
            val title = token?.coin?.code
            if (title != null) {
                subhead1_leah(text = title)
            } else {
                subhead1_jacob(text = stringResource(R.string.Swap_TokenSelectorTitle))
            }
            Icon(
                modifier = Modifier.padding(start = 4.dp),
                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
        SwapAmountInputNew(
            modifier = Modifier.weight(1f),
            viewModel = viewModel,
            amountEnabled = amountEnabled,
            focusRequester = focusRequester,
            onFocusChanged = onFocusChanged
        )
    }

}

@Composable
fun SwapAmountInputNew(
    modifier: Modifier = Modifier,
    viewModel: SwapCoinCardViewModel,
    amountEnabled: Boolean,
    focusRequester: FocusRequester,
    onFocusChanged: ((Boolean) -> Unit)?
) {
    val amountData by viewModel.amountLiveData().observeAsState()
    val secondaryInfo by viewModel.secondaryInfoLiveData().observeAsState()

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    LaunchedEffect(amountData?.first) {
        val amount = amountData?.second ?: ""
        if (!amountsEqual(amount.toBigDecimalOrNull(), textState.text.toBigDecimalOrNull())) {
            textState = textState.copy(text = amount, selection = TextRange(amount.length))
        }
    }

    Column(modifier = modifier.height(46.dp), verticalArrangement = Arrangement.Center) {
        BasicTextField(
            modifier = Modifier
                .onFocusChanged {
                    onFocusChanged?.invoke(it.isFocused)
                }
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            value = textState,
            enabled = amountEnabled,
            singleLine = true,
            onValueChange = { textFieldValue ->
                if (viewModel.isValid(textFieldValue.text)) {
                    textState = textFieldValue
                    viewModel.onChangeAmount(textFieldValue.text)
                }
            },
            textStyle = ColoredTextStyle(
                color = ComposeAppTheme.colors.leah,
                textStyle = ComposeAppTheme.typography.headline1,
                textAlign = TextAlign.End
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
            visualTransformation = { text ->
                if (text.isEmpty() || viewModel.inputParams.primaryPrefix == null) {
                    TransformedText(text, OffsetMapping.Identity)
                } else {
                    val prefix = viewModel.inputParams.primaryPrefix ?: ""
                    val out = prefix + text
                    val prefixOffset = prefix.length

                    val offsetTranslator = object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int {
                            return offset + prefixOffset
                        }
                        override fun transformedToOriginal(offset: Int): Int {
                            if (offset <= prefixOffset - 1) return prefixOffset
                            return offset - prefixOffset
                        }
                    }
                    TransformedText(AnnotatedString(out), offsetTranslator)
                }
            },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (textState.text.isEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = viewModel.inputParams.primaryPrefix ?: "0",
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.headline1,
                            textAlign = TextAlign.End
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = viewModel.inputParams.switchEnabled,
                    onClick = { viewModel.onSwitch() }
                ),
            text = secondaryInfo ?: "",
            style = ComposeAppTheme.typography.subhead2.copy(textAlign = TextAlign.End),
            color = ComposeAppTheme.colors.grey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SwapCoinCardView(
    modifier: Modifier = Modifier,
    title: String,
    viewModel: SwapCoinCardViewModel,
    uuid: Long,
    amountEnabled: Boolean,
    navController: NavController,
) {
    val token by viewModel.tokenCodeLiveData().observeAsState()
    val balance by viewModel.balanceLiveData().observeAsState()
    val balanceError by viewModel.balanceErrorLiveData().observeAsState(false)

    navController.getNavigationResult(SelectSwapCoinFragment.resultBundleKey) { bundle ->
        val requestId = bundle.getLong(SelectSwapCoinFragment.requestIdKey)
        val coinBalanceItem = bundle.getParcelable<SwapMainModule.CoinBalanceItem>(
            SelectSwapCoinFragment.coinBalanceItemResultKey
        )
        if (requestId == uuid && coinBalanceItem != null) {
            viewModel.onSelectCoin(coinBalanceItem.token)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinImage(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp),
                iconUrl = token?.coin?.iconUrl,
                placeholder = token?.iconPlaceholder ?: R.drawable.coin_placeholder
            )
            body_leah(text = title)
            Spacer(Modifier.weight(1f))
//            SelectButton(
//                title = token?.coin?.code,
//                onClick = {
//                    val params = SelectSwapCoinFragment.prepareParams(uuid, viewModel.dex)
//                    navController.slideFromBottom(R.id.selectSwapCoinDialog, params)
//                }
//            )
        }

        SwapAmountInput(
            modifier = Modifier.padding(horizontal = 8.dp),
            viewModel = viewModel,
            amountEnabled = amountEnabled
        )

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (balanceError) {
                subhead2_lucian(text = stringResource(R.string.Swap_Balance))
                Spacer(Modifier.weight(1f))
                subhead2_lucian(text = balance ?: "")
            } else {
                subhead2_grey(text = stringResource(R.string.Swap_Balance))
                Spacer(Modifier.weight(1f))
                subhead2_grey(text = balance ?: "")
            }
        }
    }
}

@Composable
fun SwapAmountInput(
    modifier: Modifier = Modifier,
    viewModel: SwapCoinCardViewModel,
    amountEnabled: Boolean
) {

    val amountData by viewModel.amountLiveData().observeAsState()
    val estimated by viewModel.isEstimatedLiveData().observeAsState(false)
    val maxVisible by viewModel.maxEnabledLiveData().observeAsState(false)
    val warningInfo by viewModel.warningInfoLiveData().observeAsState()
    val secondaryInfo by viewModel.secondaryInfoLiveData().observeAsState()

    val borderColor = if (warningInfo != null) {
        ComposeAppTheme.colors.yellow50
    } else {
        ComposeAppTheme.colors.steel20
    }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    val inputTextColor = when (viewModel.inputParams.amountType) {
        AmountTypeSwitchService.AmountType.Currency -> {
            ComposeAppTheme.colors.jacob
        }
        AmountTypeSwitchService.AmountType.Coin -> {
            ComposeAppTheme.colors.leah
        }
    }

    LaunchedEffect(amountData?.first) {
        val amount = amountData?.second ?: ""
        if (!amountsEqual(
                amount.toBigDecimalOrNull(),
                textState.text.toBigDecimalOrNull()
            )
        ) {
            textState = textState.copy(text = amount, selection = TextRange(amount.length))
        }
    }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .background(ComposeAppTheme.colors.lawrence),
        ) {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    value = textState,
                    enabled = amountEnabled,
                    singleLine = true,
                    onValueChange = { textFieldValue ->
                        if (viewModel.isValid(textFieldValue.text)) {
                            textState = textFieldValue
                            viewModel.onChangeAmount(textFieldValue.text)
                        }
                    },
                    textStyle = ColoredTextStyle(
                        color = inputTextColor,
                        textStyle = ComposeAppTheme.typography.headline2
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
                    decorationBox = { innerTextField ->
                        Row {
                            viewModel.inputParams.primaryPrefix?.let {
                                Text(
                                    modifier = Modifier.padding(end = 4.dp),
                                    text = it,
                                    color = inputTextColor,
                                    style = ComposeAppTheme.typography.headline2
                                )
                            }
                            Box {
                                if (textState.text.isEmpty()) {
                                    body_grey50(
                                        "0",
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )
                if (estimated) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(top = 8.dp, end = 16.dp)
                    ) {
                        Badge(text = stringResource(R.string.Swap_Estimated))
                    }
                }

                if (textState.text.isNotEmpty()) {
                    if (!estimated) {
                        ButtonSecondaryCircle(
                            modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                            icon = R.drawable.ic_delete_20,
                            onClick = {
                                textState = textState.copy(text = "")

                                viewModel.onChangeAmount(textState.text)
                            }
                        )
                    }
                } else if (maxVisible) {
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                        title = stringResource(R.string.Send_Button_Max),
                        onClick = {
                            viewModel.onTapMax()
                        }
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = ComposeAppTheme.colors.steel10
            )

            Row(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = viewModel.inputParams.switchEnabled,
                        onClick = { viewModel.onSwitch() }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = secondaryInfo ?: "",
                    style = ComposeAppTheme.typography.subhead2,
                    color = getSecondaryTextColor(
                        viewModel.inputParams.amountType,
                        viewModel.inputParams.switchEnabled
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        warningInfo?.let { caution ->
            caption_jacob(
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                text = caution
            )
        }
    }
}

private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
    return when {
        amount1 == null && amount2 == null -> true
        amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
        else -> false
    }
}

@Composable
private fun getSecondaryTextColor(
    type: AmountTypeSwitchService.AmountType,
    switchEnabled: Boolean
): Color {
    return when {
        !switchEnabled -> ComposeAppTheme.colors.grey50
        type == AmountTypeSwitchService.AmountType.Coin -> ComposeAppTheme.colors.jacob
        else -> ComposeAppTheme.colors.leah
    }
}
