package cash.p.terminal.ui_compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.R
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun SearchField(
    onSearchTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    var showClearButton by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .height(40.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(24.dp),
            painter = painterResource(id = R.drawable.ic_search),
            contentDescription = "search",
            tint = ComposeAppTheme.colors.jacob
        )
        BasicTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                onSearchTextChanged.invoke(it)
                showClearButton = it.isNotEmpty()
            },
            textStyle = ComposeAppTheme.typography.body.copy(
                color = ComposeAppTheme.colors.leah,
            ),
            maxLines = 1,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
            }),
            cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
            modifier = Modifier
                .padding(end = 16.dp)
                .focusRequester(focusRequester)
                .weight(1f)
        )
        AnimatedVisibility(
            visible = showClearButton,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            HsIconButton(onClick = {
                searchText = ""
                onSearchTextChanged.invoke("")
                showClearButton = false
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24),
                    contentDescription = stringResource(R.string.Button_Cancel),
                    tint = ComposeAppTheme.colors.leah.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchFieldPreview() {
    ComposeAppTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(Color.Black)
                .padding(16.dp)
        ) {
            SearchField(
                onSearchTextChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
