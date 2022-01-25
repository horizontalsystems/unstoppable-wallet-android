package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

data class MenuItem(
    val title: TranslatableString,
    @DrawableRes val icon: Int? = null,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

@Composable
fun AppBarMenuButton(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    description: String? = null,
    enabled: Boolean = true,
    tint: Color = ComposeAppTheme.colors.jacob,
) {
    IconButton(
        onClick = { onClick() },
        enabled = enabled
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = description,
            tint = tint
        )
    }
}

@Composable
fun AppBar(
    title: TranslatableString,
    navigationIcon: @Composable (() -> Unit)? = null,
    menuItems: List<MenuItem> = listOf(),
    showSpinner: Boolean = false
) {
    TopAppBar(
        modifier = Modifier.height(56.dp),
        title = {
            Text(
                text = title.getString(),
                style = ComposeAppTheme.typography.title3,
                color = ComposeAppTheme.colors.oz,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
        navigationIcon = navigationIcon?.let {
            {
                navigationIcon()
            }
        },
        actions = {
            menuItems.forEach { menuItem ->
                if (menuItem.icon != null) {
                    AppBarMenuButton(
                        icon = menuItem.icon,
                        onClick = menuItem.onClick,
                        enabled = menuItem.enabled
                    )
                } else {
                    val color = if (menuItem.enabled) {
                        ComposeAppTheme.colors.jacob
                    } else {
                        ComposeAppTheme.colors.grey50
                    }

                    Text(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable(
                                enabled = menuItem.enabled,
                                onClick = menuItem.onClick
                            ),
                        text = menuItem.title.getString(),
                        style = ComposeAppTheme.typography.headline2,
                        color = color
                    )
                }
            }
            if (showSpinner){
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = 24.dp, end = 16.dp)
                        .size(24.dp),
                    color = ComposeAppTheme.colors.grey,
                    strokeWidth = 2.dp
                )
            }
        },
        elevation = 0.dp
    )
}
