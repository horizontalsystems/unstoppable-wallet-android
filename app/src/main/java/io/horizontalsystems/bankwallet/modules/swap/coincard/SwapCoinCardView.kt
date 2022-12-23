package io.horizontalsystems.bankwallet.modules.swap.coincard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinFragment
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.core.getNavigationResult
import java.math.BigDecimal

@Composable
fun SwapCoinCardView(
    modifier: Modifier = Modifier,
    viewModel: SwapCoinCardViewModel,
    uuid: Long,
    amountEnabled: Boolean,
    navController: NavController,
    focusRequester: FocusRequester = remember { FocusRequester() },
    isLoading: Boolean = false,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val token by viewModel.tokenCodeLiveData().observeAsState()
    val isEstimated by viewModel.isEstimatedLiveData().observeAsState(false)

    Row(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth()
    ) {
        SwapAmountInput(
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp),
            viewModel = viewModel,
            amountEnabled = amountEnabled,
            focusRequester = focusRequester,
            amountDimming = isLoading && isEstimated,
            onFocusChanged = onFocusChanged
        )
        Spacer(modifier = Modifier.width(6.dp))
        Row(
            modifier = Modifier
                .height(32.dp)
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
    }

}

@Composable
fun SwapAmountInput(
    modifier: Modifier = Modifier,
    viewModel: SwapCoinCardViewModel,
    amountEnabled: Boolean,
    focusRequester: FocusRequester,
    amountDimming: Boolean,
    onFocusChanged: ((Boolean) -> Unit)?
) {
    val amountData by viewModel.amountLiveData().observeAsState()
    val secondaryInfo by viewModel.secondaryInfoLiveData().observeAsState()
    var focused by remember { mutableStateOf(false) }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    LaunchedEffect(amountData?.first) {
        val amount = amountData?.second ?: ""
        if (!amountsEqual(amount.toBigDecimalOrNull(), textState.text.toBigDecimalOrNull())) {
            if (!amountDimming || amount.isNotEmpty())
                textState = textState.copy(text = amount, selection = TextRange(amount.length))
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        BasicTextField(
            modifier = Modifier
                .onFocusChanged { focusState ->
                    focused = focusState.isFocused
                    onFocusChanged?.invoke(focusState.isFocused)
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
                color = if (amountDimming) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.leah,
                textStyle = ComposeAppTheme.typography.headline1,
                textAlign = TextAlign.Start
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.15f to Color.Transparent,
                0.15f to ComposeAppTheme.colors.jacob,
                0.85f to ComposeAppTheme.colors.jacob,
                0.85f to Color.Transparent,
                1.00f to Color.Transparent
            ),
            visualTransformation = { text ->
                val prefix = amountData?.third
                if (text.isEmpty() || prefix == null) {
                    TransformedText(text, OffsetMapping.Identity)
                } else {
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
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textState.text.isEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = viewModel.inputParams.primaryPrefix ?: "0",
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.headline1,
                            textAlign = TextAlign.Start
                        )
                        innerTextField()
                    } else if (!focused) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "${viewModel.inputParams.primaryPrefix ?: ""}${textState.text}",
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = if (amountDimming) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.leah,
                            style = ComposeAppTheme.typography.headline1,
                            textAlign = TextAlign.Start
                        )
                        Box(
                            modifier = Modifier
                                .height(0.dp)
                                .fillMaxWidth()
                        ) {
                            innerTextField()
                        }
                    } else {
                        innerTextField()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = secondaryInfo ?: "",
            style = ComposeAppTheme.typography.caption,
            textAlign = TextAlign.Start,
            color = ComposeAppTheme.colors.grey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
    return when {
        amount1 == null && amount2 == null -> true
        amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
        else -> false
    }
}
