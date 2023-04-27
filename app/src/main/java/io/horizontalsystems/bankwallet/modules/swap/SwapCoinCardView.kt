package io.horizontalsystems.bankwallet.modules.swap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.CoinBalanceItem
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapAmountInputState
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapCoinCardViewState
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinFragment
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal


@Composable
fun SwapCoinCardView(
    dex: SwapMainModule.Dex,
    cardState: SwapCoinCardViewState,
    navController: NavController,
    modifier: Modifier = Modifier,
    onCoinSelect: (Token) -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onAmountChange: ((String) -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {

    Row(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth()
    ) {
        SwapAmountInput(
            state = cardState.inputState,
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp),
            focusRequester = focusRequester,
            onFocusChanged = onFocusChanged,
            onChangeAmount = {
                onAmountChange?.invoke(it)
            }
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
                            val coinBalanceItem = bundle.getParcelable<CoinBalanceItem>(
                                SelectSwapCoinFragment.coinBalanceItemResultKey
                            )
                            if (requestId == cardState.uuid && coinBalanceItem != null) {
                                onCoinSelect.invoke(coinBalanceItem.token)
                            }
                        }

                        val params = SelectSwapCoinFragment.prepareParams(cardState.uuid, dex)
                        navController.slideFromBottom(R.id.selectSwapCoinDialog, params)
                    }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinImage(
                modifier = Modifier.size(32.dp),
                iconUrl = cardState.token?.coin?.imageUrl,
                placeholder = cardState.token?.iconPlaceholder ?: R.drawable.coin_placeholder
            )
            Spacer(Modifier.width(8.dp))
            val title = cardState.token?.coin?.code
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
private fun SwapAmountInput(
    state: SwapAmountInputState,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    onChangeAmount: (String) -> Unit,
    onFocusChanged: ((Boolean) -> Unit)?
) {
    var focused by remember { mutableStateOf(false) }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

//    LaunchedEffect(amountData?.first) {
    if (!amountsEqual(state.amount.toBigDecimalOrNull(), textState.text.toBigDecimalOrNull())) {
        if (!state.dimAmount || state.amount.isNotEmpty())
            textState = textState.copy(text = state.amount, selection = TextRange(state.amount.length))
    }
//    }

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
            enabled = state.amountEnabled,
            singleLine = true,
            onValueChange = { textFieldValue ->
                if (isValid(textFieldValue.text, state.validDecimals)) {
                    textState = textFieldValue
                    onChangeAmount.invoke(textFieldValue.text)
                }
            },
            textStyle = ColoredTextStyle(
                color = if (state.dimAmount) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.leah,
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
                if (text.isEmpty() || state.primaryPrefix == null) {
                    TransformedText(text, OffsetMapping.Identity)
                } else {
                    val out = state.primaryPrefix + text
                    val prefixOffset = state.primaryPrefix.length

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
                            text = state.primaryPrefix ?: "0",
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
                            text = "${state.primaryPrefix ?: ""}${textState.text}",
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = if (state.dimAmount) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.leah,
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
            text = state.secondaryInfo,
            style = ComposeAppTheme.typography.caption,
            textAlign = TextAlign.Start,
            color = ComposeAppTheme.colors.grey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun isValid(amount: String?, validDecimals: Int): Boolean {
    val newAmount = amount?.toBigDecimalOrNull()

    return when {
        amount.isNullOrBlank() -> true
        newAmount != null && newAmount.scale() > validDecimals -> false
        else -> true
    }
}

private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
    return when {
        amount1 == null && amount2 == null -> true
        amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
        else -> false
    }
}
