package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@Composable
fun SearchBar(
    title: String,
    searchHintText: String = "",
    searchOnlyMode: Boolean = false,
    searchModeInitial: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    menuItems: List<MenuItem> = listOf(),
    onClose: () -> Unit,
    onSearchTextChanged: (String) -> Unit = {},
) {

    var searchMode by remember { mutableStateOf(searchModeInitial) }
    var showClearButton by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchText by remember { mutableStateOf("") }

    TopAppBar(
        modifier = Modifier
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .height(64.dp),
        title = {
            title3_leah(
                text = if (searchMode) "" else title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
        elevation = 0.dp,
        navigationIcon = {
            HsIconButton(onClick = {
                if (searchMode && !searchOnlyMode) {
                    searchText = ""
                    onSearchTextChanged.invoke("")
                    searchMode = false
                } else {
                    onClose.invoke()
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = stringResource(R.string.Button_Back),
                    tint = ComposeAppTheme.colors.jacob
                )
            }
        },
        actions = {
            if (searchMode) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp)
                        .focusRequester(focusRequester),
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        onSearchTextChanged.invoke(it)
                        showClearButton = it.isNotEmpty()
                    },
                    placeholder = {
                        body_grey50(
                            text = searchHintText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    textStyle = ComposeAppTheme.typography.body,
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        backgroundColor = Color.Transparent,
                        cursorColor = ComposeAppTheme.colors.jacob,
                        textColor = ComposeAppTheme.colors.leah
                    ),
                    maxLines = 1,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                    }),
                    trailingIcon = {
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
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.Button_Cancel),
                                    tint = ComposeAppTheme.colors.jacob
                                )
                            }

                        }
                    },
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }

            if (!searchMode) {
                AppBarMenuButton(
                    icon = R.drawable.ic_search,
                    onClick = { searchMode = true },
                    description = stringResource(R.string.Button_Search),
                )

                menuItems.forEach { menuItem ->
                    if (menuItem.icon != null) {
                        AppBarMenuButton(
                            icon = menuItem.icon,
                            onClick = menuItem.onClick,
                            description = menuItem.title.getString(),
                            enabled = menuItem.enabled,
                            tint = menuItem.tint
                        )
                    } else {
                        Text(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable(
                                    enabled = menuItem.enabled,
                                    onClick = menuItem.onClick
                                ),
                            text = menuItem.title.getString(),
                            style = ComposeAppTheme.typography.headline2,
                            color = if (menuItem.enabled) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.yellow50
                        )
                    }
                }
            }
        })

}
