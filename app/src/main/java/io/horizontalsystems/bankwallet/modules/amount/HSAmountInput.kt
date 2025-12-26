package io.horizontalsystems.bankwallet.modules.amount

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.address.AmountUnique
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.animations.shake
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50
import java.math.BigDecimal

/**
 * @param amountUnique used special class [AmountUnique] to be able to set the same amount again. It won't work with BigDecimal
 */
@Composable
fun HSAmountInput(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    availableBalance: BigDecimal,
    caution: HSCaution? = null,
    coinCode: String,
    coinDecimal: Int,
    fiatDecimal: Int,
    onClickHint: () -> Unit,
    onValueChange: (BigDecimal?) -> Unit,
    inputType: AmountInputType,
    rate: CurrencyValue?,
    amountUnique: AmountUnique? = null
) {
    val viewModel = viewModel<AmountInputViewModel2>(
        factory = AmountInputModule.Factory(
            coinCode,
            coinDecimal,
            fiatDecimal,
            inputType
        )
    )
    LaunchedEffect(availableBalance) {
        viewModel.setAvailableBalance(availableBalance)
    }
    LaunchedEffect(rate) {
        viewModel.setRate(rate)
    }

    val hint = viewModel.hint

    val borderColor = when (caution?.type) {
        HSCaution.Type.Error -> ComposeAppTheme.colors.red50
        HSCaution.Type.Warning -> ComposeAppTheme.colors.yellow50
        else -> ComposeAppTheme.colors.blade
    }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    var playShakeAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(inputType) {
        viewModel.setInputType(inputType)

        val text = viewModel.getEnterAmount()
        textState = textState.copy(text = text, selection = TextRange(text.length))
    }

    LaunchedEffect(amountUnique) {
        amountUnique?.let {
            viewModel.setCoinAmountExternal(amountUnique.amount)

            val text = viewModel.getEnterAmount()
            textState = textState.copy(text = text, selection = TextRange(text.length))

            onValueChange.invoke(viewModel.coinAmount)
        }
    }

    val inputTextColor: Color
    val hintTextColor: Color

    when (inputType) {
        AmountInputType.COIN -> {
            inputTextColor = ComposeAppTheme.colors.leah
            hintTextColor = ComposeAppTheme.colors.jacob
        }
        AmountInputType.CURRENCY -> {
            inputTextColor = ComposeAppTheme.colors.jacob
            hintTextColor = ComposeAppTheme.colors.leah
        }
    }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
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
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .shake(
                            enabled = playShakeAnimation,
                            onAnimationFinish = { playShakeAnimation = false }
                        ),
                    value = textState,
                    singleLine = true,
                    onValueChange = { textFieldValue ->
                        val text = textFieldValue.text
                        if (viewModel.isValid(text)) {
                            textState = textFieldValue

                            viewModel.onEnterAmount(text)
                            onValueChange.invoke(viewModel.coinAmount)
                        } else {
                            playShakeAnimation = true
                        }
                    },
                    textStyle = ColoredTextStyle(
                        color = inputTextColor,
                        textStyle = ComposeAppTheme.typography.headline2
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
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

                if (textState.text.isNotEmpty()) {
                    ButtonSecondaryCircle(
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                        icon = R.drawable.ic_delete_20,
                        onClick = {
                            textState = textState.copy(text = "")

                            viewModel.onEnterAmount(textState.text)
                            onValueChange.invoke(viewModel.coinAmount)
                        }
                    )
                } else if (viewModel.isMaxEnabled) {
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                        title = stringResource(R.string.Send_Button_Max),
                        onClick = {
                            viewModel.onClickMax()
                            val text = viewModel.getEnterAmount()
                            textState =
                                textState.copy(text = text, selection = TextRange(text.length))

                            onValueChange.invoke(viewModel.coinAmount)
                        }
                    )
                }
            }

            HsDivider(modifier = Modifier.padding(horizontal = 8.dp))

            Row(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClickHint
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = hint ?: stringResource(R.string.NotAvailable),
                    style = ComposeAppTheme.typography.subheadR,
                    color = if (hint == null) ComposeAppTheme.colors.andy else hintTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        caution?.let { caution ->
            val color: Color = when (caution.type) {
                HSCaution.Type.Error -> ComposeAppTheme.colors.redD
                HSCaution.Type.Warning -> ComposeAppTheme.colors.yellowD
            }
            Text(
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                text = caution.getString(),
                style = ComposeAppTheme.typography.caption,
                color = color,
            )
        }
    }
}