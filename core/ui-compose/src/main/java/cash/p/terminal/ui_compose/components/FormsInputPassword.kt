package cash.p.terminal.ui_compose.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cash.p.terminal.resources.R
import cash.p.terminal.ui_compose.entities.DataState
import cash.p.terminal.ui_compose.entities.FormsInputStateWarning
import cash.p.terminal.ui_compose.theme.ColoredTextStyle
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun FormsInputPassword(
    modifier: Modifier = Modifier,
    hint: String,
    textColor: Color = ComposeAppTheme.colors.leah,
    textStyle: TextStyle = ComposeAppTheme.typography.body,
    hintColor: Color = ComposeAppTheme.colors.grey50,
    hintStyle: TextStyle = ComposeAppTheme.typography.body,
    singleLine: Boolean = true,
    state: DataState<Any>? = null,
    maxLength: Int? = null,
    hide: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
    onToggleHide: () -> Unit
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

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .background(ComposeAppTheme.colors.lawrence),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(""))
            }

            BasicTextField(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .weight(1f),
                value = textState,
                onValueChange = { textFieldValue ->

                    val text = textFieldValue.text
                    if (maxLength == null || text.length <= maxLength) {
                        textState = textFieldValue
                        onValueChange.invoke(text)
                    } else {
                        // Need to set textState to new instance of TextFieldValue with the same values
                        // Otherwise it getting set to empty string
                        textState = TextFieldValue(text = textState.text, selection = textState.selection)
                    }
                },
                textStyle = ColoredTextStyle(
                    color = textColor,
                    textStyle = textStyle
                ),
                singleLine = singleLine,
                cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
                decorationBox = { innerTextField ->
                    if (textState.text.isEmpty()) {
                        Text(
                            hint,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = hintColor,
                            style = hintStyle
                        )
                    }
                    innerTextField()
                },
                visualTransformation = if (hide) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = keyboardOptions,
                enabled = enabled,
            )

            when (state) {
                is DataState.Error -> {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        painter = painterResource(id = R.drawable.ic_attention_20),
                        contentDescription = null,
                        tint = cautionColor
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.width(28.dp))
                }
            }

            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onToggleHide, interactionSource = MutableInteractionSource(), indication = null),
                painter = painterResource(id = if (hide) R.drawable.ic_eye_off_20 else R.drawable.ic_eye_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
            Spacer(Modifier.width(16.dp))
        }

        state?.errorOrNull?.localizedMessage?.let {
            Text(
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                text = it,
                color = cautionColor,
                style = ComposeAppTheme.typography.caption
            )
        }
    }
}
