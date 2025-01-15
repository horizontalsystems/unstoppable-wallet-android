package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

sealed class IMenuItem

data class MenuItemTimeoutIndicator(
    val progress: Float
) : IMenuItem()

data class MenuItem(
    val title: TranslatableString,
    @DrawableRes val icon: Int? = null,
    val enabled: Boolean = true,
    val tint: Color = Color.Unspecified,
    val showAlertDot: Boolean = false,
    val onClick: () -> Unit,
) : IMenuItem()

@Composable
fun AppBarMenuButton(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    description: String,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    showAlertDot: Boolean = false
) {
    HsIconButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(modifier = Modifier.size(30.dp)) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center),
                painter = painterResource(id = icon),
                contentDescription = description,
                tint = tint
            )
            if (showAlertDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .background(ComposeAppTheme.colors.lucian, shape = CircleShape)
                )
            }
        }
    }
}

@Composable
fun AppBar(
    title: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    menuItems: List<IMenuItem> = listOf(),
    showSpinner: Boolean = false,
    backgroundColor: Color = ComposeAppTheme.colors.tyler
) {
    val titleComposable: @Composable () -> Unit = {
        title?.let {
            title3_leah(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    AppBar(
        title = titleComposable,
        navigationIcon = navigationIcon,
        menuItems = menuItems,
        showSpinner = showSpinner,
        backgroundColor = backgroundColor
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable (() -> Unit)? = null,
    menuItems: List<IMenuItem> = listOf(),
    showSpinner: Boolean = false,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    backgroundColor: Color = ComposeAppTheme.colors.tyler
) {
    TopAppBar(
        modifier = Modifier
            .windowInsetsPadding(windowInsets)
            .height(64.dp),
        title = title,
        backgroundColor = backgroundColor,
        navigationIcon = navigationIcon?.let {
            {
                navigationIcon()
            }
        },
        actions = {
            if (showSpinner) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = 24.dp, end = 16.dp)
                        .size(24.dp),
                    color = ComposeAppTheme.colors.jacob,
                    strokeWidth = 2.dp
                )
            }
            menuItems.forEach { menuItem ->
                when (menuItem) {
                    is MenuItem -> {
                        MenuItemSimple(menuItem)
                    }
                    is MenuItemTimeoutIndicator -> {
                        HsIconButton(
                            onClick = {  }
                        ) {
                            Box(
                                modifier = Modifier.size(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = 1f,
                                    modifier = Modifier.size(16.dp),
                                    color = ComposeAppTheme.colors.steel20,
                                    strokeWidth = 1.5.dp
                                )
                                CircularProgressIndicator(
                                    progress = menuItem.progress,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .scale(scaleX = -1f, scaleY = 1f),
                                    color = ComposeAppTheme.colors.jacob,
                                    strokeWidth = 1.5.dp
                                )
                            }
                        }
                    }
                }
            }
        },
        elevation = 0.dp
    )
}

@Composable
private fun MenuItemSimple(menuItem: MenuItem) {
    val color = if (menuItem.enabled) {
        if (menuItem.tint == Color.Unspecified)
            ComposeAppTheme.colors.jacob
        else
            menuItem.tint
    } else {
        ComposeAppTheme.colors.grey50
    }

    val icon = menuItem.icon
    if (icon != null) {
        AppBarMenuButton(
            icon = icon,
            onClick = menuItem.onClick,
            enabled = menuItem.enabled,
            tint = color,
            description = menuItem.title.getString(),
            showAlertDot = menuItem.showAlertDot,
        )
    } else {
        ButtonPrimaryWrapper(
            enabled = menuItem.enabled,
            onClick = menuItem.onClick
        ){
            Text(
                text = menuItem.title.getString().uppercase(),
                color = color
            )
        }
    }
}
