package io.horizontalsystems.designkit.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import io.horizontalsystems.designkit.R
import io.horizontalsystems.designkit.theme.ComposeAppTheme

enum class InputType {
    /** One row: the text with its controls to the right. */
    Field,

    /** The text on top with its controls in a row underneath. */
    Area
}

enum class InputState {
    Default, Error, Warning
}

/**
 * The bordered text input. It holds no text of its own: the caller owns
 * [value] and decides what [onValueChange] accepts.
 *
 * @param description the line under the input. It takes the colour of
 * [state], so it carries the error or warning message as well as a hint.
 * @param textFieldModifier applied to the text field itself, for focus
 * requesters and focus callbacks.
 * @param leading shown before the text in a [InputType.Field].
 * @param trailing the controls of the input, laid out 12 dp apart: status
 * icons first, then buttons.
 */
@Composable
fun HSInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    type: InputType = InputType.Field,
    hint: String? = null,
    state: InputState = InputState.Default,
    description: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    textStyle: TextStyle = ComposeAppTheme.typography.body,
    textColor: Color = ComposeAppTheme.colors.leah,
    hintStyle: TextStyle = ComposeAppTheme.typography.body,
    hintColor: Color = ComposeAppTheme.colors.andy,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textFieldModifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = ComposeAppTheme.colors
    val stateColor = when (state) {
        InputState.Default -> null
        InputState.Error -> colors.lucian
        InputState.Warning -> colors.jacob
    }
    val shape = RoundedCornerShape(16.dp)
    val frame = Modifier
        .fillMaxWidth()
        .clip(shape)
        .border(1.dp, stateColor ?: colors.andy, shape)

    val textField: @Composable (Modifier) -> Unit = { textModifier ->
        BasicTextField(
            modifier = textModifier.then(textFieldModifier),
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = textStyle.copy(color = textColor),
            singleLine = singleLine,
            maxLines = maxLines,
            cursorBrush = SolidColor(colors.leah),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->
                if (value.text.isEmpty() && hint != null) {
                    Text(
                        text = hint,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (type == InputType.Field) 1 else Int.MAX_VALUE,
                        color = hintColor,
                        style = hintStyle
                    )
                }
                innerTextField()
            },
        )
    }

    Column(modifier) {
        when (type) {
            InputType.Field -> Row(
                modifier = frame.defaultMinSize(minHeight = 64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leading?.let {
                    Box(Modifier.padding(start = 16.dp)) { it() }
                }
                textField(
                    Modifier
                        .weight(1f)
                        .padding(16.dp)
                )
                trailing?.let {
                    Row(
                        modifier = Modifier.paddingUnlessEmpty(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = it,
                    )
                }
            }

            InputType.Area -> Column(frame) {
                textField(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                trailing?.let {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .paddingUnlessEmpty(end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        content = it,
                    )
                }
            }
        }

        description?.let {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                text = it,
                color = stateColor ?: colors.grey,
                style = ComposeAppTheme.typography.caption
            )
        }
    }
}

/**
 * [HSInput] for callers that keep the text as a plain string. The selection
 * and composition are remembered here, the way BasicTextField does it for
 * its own string overload.
 */
@Composable
fun HSInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    type: InputType = InputType.Field,
    hint: String? = null,
    state: InputState = InputState.Default,
    description: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    textStyle: TextStyle = ComposeAppTheme.typography.body,
    textColor: Color = ComposeAppTheme.colors.leah,
    hintStyle: TextStyle = ComposeAppTheme.typography.body,
    hintColor: Color = ComposeAppTheme.colors.andy,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textFieldModifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val textFieldValue = textFieldValueState.copy(text = value)

    SideEffect {
        if (textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }

    var lastTextValue by remember(value) { mutableStateOf(value) }

    HSInput(
        value = textFieldValue,
        onValueChange = { newTextFieldValue ->
            textFieldValueState = newTextFieldValue

            val changed = lastTextValue != newTextFieldValue.text
            lastTextValue = newTextFieldValue.text

            if (changed) {
                onValueChange(newTextFieldValue.text)
            }
        },
        modifier = modifier,
        type = type,
        hint = hint,
        state = state,
        description = description,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = textStyle,
        textColor = textColor,
        hintStyle = hintStyle,
        hintColor = hintColor,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textFieldModifier = textFieldModifier,
        leading = leading,
        trailing = trailing,
    )
}

/**
 * Pads the controls only when there are any. Which controls an input shows
 * depends on its text and state, and a row that shows none must not leave a
 * gap behind.
 */
private fun Modifier.paddingUnlessEmpty(end: Dp = 0.dp, bottom: Dp = 0.dp) =
    layout { measurable, constraints ->
        val endPx = end.roundToPx()
        val bottomPx = bottom.roundToPx()
        val placeable = measurable.measure(constraints.offset(-endPx, -bottomPx))

        if (placeable.height == 0) {
            layout(0, 0) {}
        } else {
            layout(placeable.width + endPx, placeable.height + bottomPx) {
                placeable.placeRelative(0, 0)
            }
        }
    }

@Preview
@Composable
private fun Preview_HSInput() {
    ComponentPreview {
        InputType.entries.forEach { type ->
            InputState.entries.forEach { state ->
                HSInput(
                    value = TextFieldValue(if (state == InputState.Default) "" else "Text"),
                    onValueChange = {},
                    type = type,
                    hint = "Text",
                    state = state,
                    description = if (state == InputState.Default) null else "Description Text",
                ) {
                    HSIconButton(
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Small,
                        icon = painterResource(R.drawable.arrow_s_down_24),
                        onClick = {}
                    )
                    HSButton(
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Small,
                        title = "Paste"
                    )
                }
            }
        }
    }
}
