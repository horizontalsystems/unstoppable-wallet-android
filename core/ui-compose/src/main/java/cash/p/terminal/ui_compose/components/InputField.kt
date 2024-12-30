package cash.p.terminal.ui_compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.R
import cash.p.terminal.ui_compose.theme.ColoredTextStyle
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
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
                        .focusRequester(focusRequester),
                    value = value,
                    singleLine = true,
                    onValueChange = onValueChange,
                    textStyle = ColoredTextStyle(
                        color = ComposeAppTheme.colors.leah,
                        textStyle = ComposeAppTheme.typography.headline2
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                body_grey50(
                                    text = placeholderText,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (value.isNotEmpty()) {
                    ButtonSecondaryCircle(
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                        icon = R.drawable.ic_delete_20,
                        onClick = {
                            onValueChange("")
                        }
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF888888,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    showBackground = true,
    backgroundColor = 0,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun InputFieldPreview() {
    ComposeAppTheme {
        InputField(
            value = "",
            onValueChange = {},
            placeholderText = "Enter Coin Amount"
        )
    }
}