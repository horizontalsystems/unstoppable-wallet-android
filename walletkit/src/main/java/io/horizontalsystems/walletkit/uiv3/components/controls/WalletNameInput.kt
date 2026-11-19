package io.horizontalsystems.walletkit.uiv3.components.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.horizontalsystems.designkit.components.ButtonSize
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSIconButton
import io.horizontalsystems.designkit.components.HSInput
import io.horizontalsystems.walletkit.R

/**
 * The name field of the screens that add a wallet. Its button fills in a random name; while the
 * field is focused and holds text, the button clears it instead.
 *
 * The field owns its text after [initial], so the caller follows it through [onValueChange].
 * A name the caller picks by itself later goes in [assignedName] and replaces the text when it
 * changes, without a call to [onValueChange].
 */
@Composable
fun WalletNameInput(
    initial: String,
    hint: String,
    onValueChange: (String) -> Unit,
    randomName: () -> String,
    assignedName: String? = null,
    modifier: Modifier = Modifier,
    randomModifier: Modifier = Modifier,
    clearModifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }

    // Applied while composing, not in an effect, so that no keystroke lands between the caller
    // assigning a name and the field showing it.
    var shownAssignedName by rememberSaveable { mutableStateOf<String?>(null) }
    if (assignedName != shownAssignedName) {
        shownAssignedName = assignedName
        if (assignedName != null && assignedName != textState.text) {
            textState = TextFieldValue(assignedName, TextRange(assignedName.length))
        }
    }

    // Text restored into a recreated process is unknown to the caller, which starts from [initial].
    LaunchedEffect(Unit) {
        val text = textState.text
        if (text != initial && text != assignedName) {
            onValueChange(text)
        }
    }

    fun setText(text: String) {
        textState = TextFieldValue(text, TextRange(text.length))
        onValueChange(text)
    }

    HSInput(
        modifier = modifier,
        value = textState,
        onValueChange = {
            textState = it
            onValueChange(it.text)
        },
        hint = hint,
        textFieldModifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused },
    ) {
        if (focused && textState.text.isNotEmpty()) {
            HSIconButton(
                modifier = clearModifier,
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
                icon = painterResource(R.drawable.ic_delete_20),
                onClick = {
                    setText("")
                    focusRequester.requestFocus()
                }
            )
        } else {
            HSIconButton(
                modifier = randomModifier,
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
                icon = painterResource(R.drawable.magic_24),
                onClick = { setText(randomName()) }
            )
        }
    }
}
