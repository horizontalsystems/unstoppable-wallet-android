package io.horizontalsystems.bankwallet.modules.swap.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapDeadlineViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSlippageViewModel
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputStateWarning
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

@Composable
fun SlippageAmount(
    slippageViewModel: SwapSlippageViewModel
) {
    SlippageAmount(
        slippageViewModel.inputFieldPlaceholder,
        slippageViewModel.initialValue,
        slippageViewModel.inputButtons,
        slippageViewModel.errorState?.error
    ) {
        slippageViewModel.onChangeText(it)
    }
}

@Composable
fun SlippageAmount(
    hint: String?,
    initial: String?,
    buttons: List<InputButton>,
    error: Throwable?,
    onValueChange: (String) -> Unit
) {
    HeaderText(
        text = stringResource(R.string.SwapSettings_SlippageTitle)
    )
    InputWithButtons(
        modifier = Modifier.padding(horizontal = 16.dp),
        hint = hint,
        initial = initial,
        buttons = buttons,
        state = error?.let { DataState.Error(it) },
        onValueChange = onValueChange
    )
    InfoText(
        text = stringResource(R.string.SwapSettings_SlippageDescription),
    )
}

@Composable
fun TransactionDeadlineInput(deadlineViewModel: SwapDeadlineViewModel) {
    TransactionDeadlineInput(
        deadlineViewModel.inputFieldPlaceholder,
        deadlineViewModel.initialValue,
        deadlineViewModel.inputButtons,
        deadlineViewModel.errorState?.error
    ) {
        deadlineViewModel.onChangeText(it)
    }
}

@Composable
fun TransactionDeadlineInput(
    hint: String,
    initial: String?,
    buttons: List<InputButton>,
    error: Throwable?,
    onValueChange: (String) -> Unit,
) {
    HeaderText(
        text = stringResource(R.string.SwapSettings_DeadlineTitle)
    )
    InputWithButtons(
        modifier = Modifier.padding(horizontal = 16.dp),
        hint = hint,
        initial = initial,
        buttons = buttons,
        state = error?.let { DataState.Error(it) },
        onValueChange = onValueChange
    )
    InfoText(
        text = stringResource(R.string.SwapSettings_DeadlineDescription),
    )
}

@Composable
fun RecipientAddress(
    blockchainType: BlockchainType,
    recipientAddressViewModel: RecipientAddressViewModel,
    navController: NavController,
) {
    val tokenQuery = TokenQuery(blockchainType, TokenType.Native)
    App.marketKit.token(tokenQuery)?.let { token ->
        RecipientAddress(
            blockchainType = blockchainType,
            navController = navController,
            initial = recipientAddressViewModel.initialAddress,
            onError = {
                recipientAddressViewModel.updateError(it)
            }
        ) {
            recipientAddressViewModel.onChangeAddress(it)
        }
    }
}

@Composable
fun RecipientAddress(
    blockchainType: BlockchainType,
    navController: NavController,
    initial: Address?,
    onError: (Throwable?) -> Unit,
    onValueChange: (Address?) -> Unit,
) {
    val tokenQuery = TokenQuery(blockchainType, TokenType.Native)
    App.marketKit.token(tokenQuery)?.let { token ->
        HeaderText(
            text = stringResource(R.string.SwapSettings_RecipientAddressTitle)
        )
        HSAddressInput(
            modifier = Modifier.padding(horizontal = 16.dp),
            initial = initial,
            tokenQuery = token.tokenQuery,
            coinCode = token.coin.code,
            navController = navController,
            onError = onError,
            onValueChange = onValueChange,
        )
        InfoText(
            text = stringResource(R.string.SwapSettings_RecipientAddressDescription),
        )
    }
}

@Composable
fun InputWithButtons(
    modifier: Modifier = Modifier,
    hint: String? = null,
    initial: String? = null,
    buttons: List<InputButton>,
    state: DataState<Any>? = null,
    onValueChange: (String) -> Unit,
) {
    val borderColor = when (state) {
        is DataState.Error -> {
            if (state.error is FormsInputStateWarning) {
                ComposeAppTheme.colors.yellow50
            } else {
                ComposeAppTheme.colors.red50
            }
        }
        else -> ComposeAppTheme.colors.steel20
    }

    val cautionColor = if (state?.errorOrNull is FormsInputStateWarning) {
        ComposeAppTheme.colors.jacob
    } else {
        ComposeAppTheme.colors.lucian
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .height(44.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(initial ?: ""))
            }

            LaunchedEffect(initial) {
                if (textState.text != initial) {
                    textState = textState.copy(text = initial ?: "")
                }
            }

            BasicTextField(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .weight(1f),
                value = textState,
                onValueChange = { textValue ->
                    textState = textValue
                    onValueChange.invoke(textValue.text)
                },
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.leah,
                    textStyle = ComposeAppTheme.typography.body
                ),
                maxLines = 1,
                decorationBox = { innerTextField ->
                    if (textState.text.isEmpty()) {
                        body_grey50(
                            hint ?: "",
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                },
            )

            if (textState.text.isNotEmpty()) {
                ButtonSecondaryCircle(
                    icon = R.drawable.ic_delete_20,
                    onClick = {
                        val text = ""
                        textState = textState.copy(text = text, selection = TextRange(0))
                        onValueChange.invoke(text)
                    }
                )
            } else {
                buttons.forEachIndexed { index, button ->
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(end = if (index == buttons.size - 1) 0.dp else 8.dp),
                        title = button.title,
                        onClick = {
                            textState = textState.copy(
                                text = button.rawValue,
                                selection = TextRange(button.rawValue.length)
                            )
                            onValueChange.invoke(button.rawValue)
                        },
                    )
                }
            }

        }

        state?.errorOrNull?.localizedMessage?.let {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                text = it,
                color = cautionColor,
                style = ComposeAppTheme.typography.caption
            )
        }
    }
}

class InputButton(val title: String, val rawValue: String)
