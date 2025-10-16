package io.horizontalsystems.bankwallet.uiv3.components.bottom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton

@Composable
fun BoxScope.BottomSearchBar(
    modifier: Modifier = Modifier,
    searchQuery: String,
    isSearchActive: Boolean,
    keepCancelButton: Boolean = false,
    onActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit = { },
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Row(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .imePadding()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FloatingSearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { query ->
                onSearchQueryChange(query)
            },
            isActive = isSearchActive,
            onActiveChange = { active ->
                onActiveChange(active)
                if (active) {
                    focusManager.clearFocus(force = true)
                    keyboardController?.show()
                } else {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }
            },
            modifier = Modifier.weight(1f)
        )

        // Show clear button when there's text OR when search is active but empty
        if (keepCancelButton || searchQuery.isNotEmpty() || isSearchActive) {
            HSpacer(14.dp)
            HSIconButton(
                size = ButtonSize.Small,
                icon = painterResource(R.drawable.close_24)
            ) {
                onCloseSearch.invoke()
                onSearchQueryChange("")
                onActiveChange(false)
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }
    }
}

@Composable
fun FloatingSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var basicTextFieldHasFocus by remember { mutableStateOf(false) }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = searchQuery
        mutableStateOf(TextFieldValue(text))
    }
    LaunchedEffect(searchQuery) {
        textState = textState.copy(text = searchQuery, selection = TextRange(searchQuery.length))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ComposeAppTheme.colors.blade)
            .height(48.dp)
            .focusRequester(focusRequester)
            .clickable {
                onActiveChange(true)
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = "Search",
            tint = ComposeAppTheme.colors.grey,
            modifier = Modifier.size(24.dp)
        )
        HSpacer(8.dp)

        if (isActive || searchQuery.isNotEmpty()) {
            BasicTextField(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        basicTextFieldHasFocus = focusState.isFocused

                        if (focusState.isFocused && !isActive) {
                            onActiveChange(true)
                        }
                    },
                value = textState,
                onValueChange = { textFieldValue ->
                    val newValue = textFieldValue.text
                    onSearchQueryChange(newValue)
                    textState = textFieldValue
                },
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.leah,
                    textStyle = ComposeAppTheme.typography.body
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        body_grey(stringResource(R.string.Balance_ReceiveHint_Search))
                    }
                    innerTextField()
                },
                cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange.invoke("") },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trash_filled_24),
                        contentDescription = "Clear",
                        tint = ComposeAppTheme.colors.leah,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            body_grey(
                text = stringResource(R.string.Balance_ReceiveHint_Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            focusRequester.requestFocus()
        } else {
            if (basicTextFieldHasFocus) {
                focusManager.clearFocus()
            }
        }
    }
}