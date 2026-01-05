package io.horizontalsystems.bankwallet.uiv3.components.bars

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.R.string.Button_Back
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.IMenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemDropdown
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemLoading
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemTimeoutIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HSTopAppBar(
    title: String,
    menuItems: List<IMenuItem>,
    onBack: (() -> Unit)?,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ComposeAppTheme.colors.tyler,
            titleContentColor = ComposeAppTheme.colors.leah,
        ),
        title = {
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = title,
                style = ComposeAppTheme.typography.headline1
            )
        },
        navigationIcon = {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(40.dp),
                ) {
                    Icon(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(
                                    bounded = false,
                                    color = ComposeAppTheme.colors.leah
                                ),
                                onClick = onBack
                            ),
                        painter = painterResource(id = R.drawable.arrow_m_left_24),
                        contentDescription = stringResource(Button_Back),
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            }
        },
        actions = {
            Row(
                modifier = Modifier.padding(end = 4.dp)
            ) {
                menuItems.forEach { menuItem: IMenuItem ->
                    when (menuItem) {
                        is MenuItem -> {
                            if (menuItem.icon != null) {
                                Box(
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(
                                                    bounded = false,
                                                    color = ComposeAppTheme.colors.leah
                                                ),
                                                onClick = menuItem.onClick
                                            ),
                                        painter = painterResource(menuItem.icon),
                                        contentDescription = null,
                                        tint = menuItem.tint
                                    )
                                    if (menuItem.showAlertDot) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 5.dp, end = 5.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(ComposeAppTheme.colors.lucian),
                                            content = { }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = menuItem.title.getString().uppercase(),
                                    style = ComposeAppTheme.typography.headline2,
                                    color = if (menuItem.enabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(
                                                bounded = false,
                                                color = ComposeAppTheme.colors.leah
                                            ),
                                            enabled = menuItem.enabled,
                                            onClick = menuItem.onClick
                                        ),
                                )
                            }
                        }

                        is MenuItemTimeoutIndicator -> {
                            HsIconButton(
                                onClick = { }
                            ) {
                                Box(
                                    modifier = Modifier.size(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = 1f,
                                        modifier = Modifier.size(16.dp),
                                        color = ComposeAppTheme.colors.blade,
                                        strokeWidth = 1.5.dp
                                    )
                                    CircularProgressIndicator(
                                        progress = menuItem.progress,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .scale(scaleX = -1f, scaleY = 1f),
                                        color = ComposeAppTheme.colors.grey,
                                        strokeWidth = 1.5.dp
                                    )
                                }
                            }
                        }

                        is MenuItemLoading -> {
                            Box(
                                modifier = Modifier.size(40.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(20.dp),
                                    color = ComposeAppTheme.colors.leah,
                                    backgroundColor = ComposeAppTheme.colors.andy,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        is MenuItemDropdown -> {
                            MenuDropdown(menuItem)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun MenuDropdown(menuItemDropdown: MenuItemDropdown)     {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = false,
                        color = ComposeAppTheme.colors.leah
                    ),
                    onClick = {
                        expanded = true
                    }
                ),
            painter = painterResource(menuItemDropdown.icon),
            contentDescription = null,
            tint = Color.Unspecified
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        shape = RoundedCornerShape(16.dp),
        containerColor = ComposeAppTheme.colors.lawrence,
        modifier = Modifier.defaultMinSize(minWidth = 200.dp)
    ) {
        menuItemDropdown.items.forEachIndexed { i,  menuItem ->
            BoxBordered(top = i != 0) {
                DropdownMenuItem(
                    text = {
                        body_leah(menuItem.title.getString())
                    },
                    onClick = {
                        expanded = false
                        menuItem.onClick.invoke()
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun Preview_HSTopAppBar() {
    ComposeAppTheme {
        HSTopAppBar(
            title = "Wallet",
            menuItems = listOf(
                MenuItem(
                    icon = R.drawable.ic_balance_chart_24,
                    title = TranslatableString.ResString(R.string.Coin_Info),
                    onClick = {},
                    showAlertDot = true
                ),
                MenuItem(
                    icon = R.drawable.ic_warning_filled_24,
                    title = TranslatableString.ResString(R.string.BalanceSyncError_Title),
                    tint = ComposeAppTheme.colors.lucian,
                    onClick = {
                    }
                ),
                MenuItemLoading,
            ),
            onBack = { }
        )
    }
}