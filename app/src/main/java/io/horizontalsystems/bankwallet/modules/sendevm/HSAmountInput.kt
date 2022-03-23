package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.marketkit.models.Coin
import java.math.BigDecimal

@Composable
fun HSAmountInput(
    modifier: Modifier = Modifier,
    availableBalance: BigDecimal,
    coin: Coin,
    coinDecimal: Int,
    fiatDecimal: Int,
    amountValidator: AmountInputViewModel2.AmountValidator? = null,
    onUpdateInputMode: (AmountInputModule.InputMode) -> Unit,
    onValueChange: (BigDecimal?) -> Unit
) {
    val viewModel = viewModel<AmountInputViewModel2>(factory = AmountInputModule.Factory(coin, coinDecimal, fiatDecimal, amountValidator))
    LaunchedEffect(availableBalance) {
        viewModel.availableBalance = availableBalance
    }

    val caution = viewModel.caution

    val borderColor = when (caution?.type) {
        Caution.Type.Error -> ComposeAppTheme.colors.red50
        Caution.Type.Warning -> ComposeAppTheme.colors.yellow50
        else -> ComposeAppTheme.colors.steel20
    }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    val inputTextColor: Color
    val hintTextColor: Color

    when (viewModel.inputMode) {
        AmountInputModule.InputMode.Coin -> {
            inputTextColor = ComposeAppTheme.colors.leah
            hintTextColor = ComposeAppTheme.colors.jacob
        }
        AmountInputModule.InputMode.Currency -> {
            inputTextColor = ComposeAppTheme.colors.jacob
            hintTextColor = ComposeAppTheme.colors.leah
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
                    .fillMaxWidth()
            ) {
                BasicTextField(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 12.dp)
                        .weight(1f),
                    value = textState,
                    singleLine = true,
                    onValueChange = { textFieldValue ->
                        val text = textFieldValue.text
                        if (viewModel.isValid(text)) {
                            textState = textFieldValue

                            viewModel.onEnterAmount(text)
                            onValueChange.invoke(viewModel.getResultCoinAmount())
                        } else {
                            // todo: shake animation
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
                            viewModel.inputPrefix?.let {
                                Text(
                                    modifier = Modifier.padding(end = 4.dp),
                                    text = it,
                                    color = inputTextColor,
                                    style = ComposeAppTheme.typography.headline2
                                )
                            }
                            Box {
                                if (textState.text.isEmpty()) {
                                    Text(
                                        "0",
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        color = ComposeAppTheme.colors.grey50,
                                        style = ComposeAppTheme.typography.body
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )

                if (textState.text.isNotEmpty()) {
                    ButtonSecondaryCircle(
                        modifier = Modifier.padding(8.dp),
                        icon = R.drawable.ic_delete_20,
                        onClick = {
                            textState = textState.copy(text = "")

                            viewModel.onEnterAmount(textState.text)
                            onValueChange.invoke(viewModel.getResultCoinAmount())
                        }
                    )
                } else if (viewModel.isMaxEnabled) {
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(8.dp),
                        title = stringResource(R.string.Send_Button_Max),
                        onClick = {
                            viewModel.onClickMax()
                            val text = viewModel.getEnterAmount()
                            textState = textState.copy(text = text, selection = TextRange(text.length))

                            onValueChange.invoke(viewModel.getResultCoinAmount())
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
                        onClick = {
                            viewModel.onToggleInputMode()
                            onUpdateInputMode.invoke(viewModel.inputMode)
                            val text = viewModel.getEnterAmount()
                            textState = textState.copy(text = text, selection = TextRange(text.length))
                        },
                    )
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp, bottom = 12.dp, end = 12.dp)
                        .align(Alignment.Bottom),
                    text = viewModel.hint,
                    style = ComposeAppTheme.typography.subhead2,
                    color = hintTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        caution?.let { caution ->
            val color: Color = when (caution.type) {
                Caution.Type.Error -> ComposeAppTheme.colors.redD
                Caution.Type.Warning -> ComposeAppTheme.colors.yellowD
            }
            Text(
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                text = caution.text,
                style = ComposeAppTheme.typography.caption,
                color = color,
            )
        }
    }
}