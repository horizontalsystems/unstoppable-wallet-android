package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.ui.compose.ColoredTextStyle
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider

/**
 * A full-width single-line text cell on the send screen, closed by a divider, with a
 * caption below. The row above provides the top divider. A [value] handed in replaces the
 * typed text, which is how a locked value is shown.
 */
@Composable
fun SendInputCell(
    value: String?,
    hint: String,
    enabled: Boolean,
    keyboardType: KeyboardType,
    maxLength: Int,
    caption: String,
    captionColor: Color,
    onValueChange: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(value ?: "") }
    // Only an outside change (a locked value arriving) replaces the typed text; the value
    // echoed back after each keystroke is left alone so the IME's composition survives.
    LaunchedEffect(value) {
        if (value != text && value != text.trim().ifBlank { null }) {
            text = value ?: ""
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                enabled = enabled,
                onValueChange = { new ->
                    if (new.length <= maxLength) {
                        text = new
                        onValueChange(new)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = ColoredTextStyle(
                    color = if (enabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
                    textStyle = ComposeAppTheme.typography.body,
                ),
                cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = hint,
                            style = ComposeAppTheme.typography.body,
                            color = ComposeAppTheme.colors.andy,
                        )
                    }
                    innerTextField()
                },
            )
        }
        HsDivider(modifier = Modifier.fillMaxWidth())
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
            text = caption,
            style = ComposeAppTheme.typography.caption,
            color = captionColor,
        )
    }
}
