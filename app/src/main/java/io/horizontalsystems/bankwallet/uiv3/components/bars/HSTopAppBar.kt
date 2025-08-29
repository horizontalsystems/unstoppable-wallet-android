package io.horizontalsystems.bankwallet.uiv3.components.bars

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.IMenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemLoading
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemSimple
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemTimeoutIndicator

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
                text = title,
                style = ComposeAppTheme.typography.headline1
            )
        },
        navigationIcon = {
            if (onBack != null) {
                HsBackButton(onClick = onBack)
            }
        },
        actions = {
            menuItems.forEach { menuItem: IMenuItem ->
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ComposeAppTheme.colors.grey,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    )
}
