package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton

@Composable
fun BoxScope.FloatingSearchBarRow(
    modifier: Modifier = Modifier,
    searchQuery: String,
    isSearchActive: Boolean,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
    onSearchQueryChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onCloseSearch: () -> Unit = { },
) {
    Row(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.ime)
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
            },
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f)
        )

        // Show clear button when there's text OR when search is active but empty
        if (searchQuery.isNotEmpty() || isSearchActive) {
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
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ComposeAppTheme.colors.blade)
            .height(48.dp)
            .clickable {
                if (!isActive) {
                    onActiveChange(true)
                }
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = "Search",
            tint = ComposeAppTheme.colors.andy,
            modifier = Modifier.size(24.dp)
        )
        HSpacer(8.dp)

        if (isActive) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.leah,
                    textStyle = ComposeAppTheme.typography.body
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        body_andy(stringResource(R.string.Balance_ReceiveHint_Search))
                    }
                    innerTextField()
                }
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange.invoke("") },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_20),
                        contentDescription = "Clear",
                        tint = ComposeAppTheme.colors.leah,
                    )
                }
            }
        } else {
            body_andy(
                text = stringResource(R.string.Balance_ReceiveHint_Search),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Auto-focus when becoming active
    LaunchedEffect(isActive) {
        if (isActive) {
            focusRequester.requestFocus()
        }
    }
}